package com.bisc.flauncher.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.bisc.flauncher.R
import com.bisc.flauncher.data.db.CategoryDao
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.Category
import com.bisc.flauncher.data.model.CategoryMember
import com.bisc.flauncher.data.model.CategoryRule
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.ResolvedCategory
import com.bisc.flauncher.util.ProfileComponents
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The kinds Android apps name for themselves (ApplicationInfo.category), offered as ready-made
 *  categories. Read on the phone, no internet involved. */
enum class AndroidAppKind(val nameRes: Int, val iconKey: String) {
    GAME(R.string.category_android_game, "game"),
    AUDIO(R.string.category_android_audio, "music"),
    VIDEO(R.string.category_android_video, "video"),
    IMAGE(R.string.category_android_image, "photo"),
    SOCIAL(R.string.category_android_social, "social"),
    NEWS(R.string.category_android_news, "news"),
    MAPS(R.string.category_android_maps, "map"),
    PRODUCTIVITY(R.string.category_android_productivity, "work"),
    ACCESSIBILITY(R.string.category_android_accessibility, "accessibility")
}

data class AndroidSuggestion(val kind: AndroidAppKind, val apps: List<AppInfo>)

@Singleton
class CategoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CategoryDao,
    private val settingsRepository: com.bisc.flauncher.data.settings.SettingsRepository
) {
    val categories: Flow<List<Category>> = dao.observeAll()
    val members: Flow<List<CategoryMember>> = dao.observeMembers()

    suspend fun add(name: String, iconKey: String? = null, color: Int? = null, rule: CategoryRule? = null, ruleParam: String? = null): Long =
        dao.insert(Category(name = name, iconKey = iconKey, color = color, orderIndex = dao.maxOrderIndex() + 1, rule = rule?.name, ruleParam = ruleParam))

    /** Puts the members of a category in this order (and keeps it from now on). */
    suspend fun reorderMembers(ordered: List<CategoryMember>) = dao.reorderMembers(ordered)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)

    suspend fun reorder(ordered: List<Category>) = dao.updateAll(ordered.mapIndexed { index, c -> c.copy(orderIndex = index) })

    suspend fun setMember(categoryId: Long, type: GridItemType, payload: String, inCategory: Boolean) {
        if (inCategory) dao.insertMember(CategoryMember(categoryId = categoryId, type = type, payload = payload, orderIndex = dao.maxMemberOrder(categoryId) + 1))
        else dao.deleteMember(categoryId, type, payload)
    }

    suspend fun replaceMembers(categoryId: Long, members: List<Pair<GridItemType, String>>) =
        dao.replaceMembers(categoryId, members.mapIndexed { index, (type, payload) -> CategoryMember(categoryId = categoryId, type = type, payload = payload, orderIndex = index) })

    /** An element removed for good (a link or place deleted) leaves every category with it. */
    suspend fun forget(type: GridItemType, payload: String) = dao.deleteEverywhere(type, payload)

    /** A new category already holding [apps], for an Android suggestion. */
    suspend fun addFromSuggestion(suggestion: AndroidSuggestion): Long {
        val id = add(context.getString(suggestion.kind.nameRes), suggestion.kind.iconKey)
        replaceMembers(id, suggestion.apps.map { GridItemType.APP to it.componentName.flattenToString() })
        return id
    }

    private val installTimes = ConcurrentHashMap<String, Long>()
    private val androidKinds = ConcurrentHashMap<String, Int>()

    /** When the app came onto the phone; 0 when unknown. */
    fun installTime(packageName: String): Long = installTimes.getOrPut(packageName) {
        runCatching { context.packageManager.getPackageInfo(packageName, 0).firstInstallTime }.getOrDefault(0L)
    }

    fun androidKindOf(packageName: String): AndroidAppKind? {
        val raw = androidKinds.getOrPut(packageName) {
            runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                @Suppress("DEPRECATION")
                if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) ApplicationInfo.CATEGORY_GAME else info.category
            }.getOrDefault(ApplicationInfo.CATEGORY_UNDEFINED)
        }
        return when (raw) {
            ApplicationInfo.CATEGORY_GAME -> AndroidAppKind.GAME
            ApplicationInfo.CATEGORY_AUDIO -> AndroidAppKind.AUDIO
            ApplicationInfo.CATEGORY_VIDEO -> AndroidAppKind.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> AndroidAppKind.IMAGE
            ApplicationInfo.CATEGORY_SOCIAL -> AndroidAppKind.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AndroidAppKind.NEWS
            ApplicationInfo.CATEGORY_MAPS -> AndroidAppKind.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AndroidAppKind.PRODUCTIVITY
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && raw == ApplicationInfo.CATEGORY_ACCESSIBILITY) AndroidAppKind.ACCESSIBILITY else null
        }
    }

    /** Android's own kinds with the apps in each, for kinds with at least one app and no category
     *  of the same name yet. */
    suspend fun androidSuggestions(apps: List<AppInfo>, existingNames: Set<String>): List<AndroidSuggestion> = withContext(Dispatchers.Default) {
        val byKind = apps.groupBy { androidKindOf(ProfileComponents.real(it.componentName).packageName) }
        AndroidAppKind.entries
            .filter { context.getString(it.nameRes).lowercase() !in existingNames }
            .mapNotNull { kind -> byKind[kind]?.takeIf { it.isNotEmpty() }?.let { AndroidSuggestion(kind, it) } }
    }

    /** [resolve] kept up to date. [allApps] are all apps, [hidden] those hidden from the App list:
     *  an app put into a category on purpose stays there, but smart categories leave hidden ones out. */
    fun resolvedFlow(allApps: Flow<List<AppInfo>>, hidden: Flow<Set<String>>): Flow<List<ResolvedCategory>> {
        val launches = kotlinx.coroutines.flow.combine(
            settingsRepository.appLaunchCounts, settingsRepository.appLastLaunches, settingsRepository.launchTrackingSince
        ) { counts, last, since -> Triple(counts, last, since) }
        return kotlinx.coroutines.flow.combine(categories, members, allApps, hidden, launches) { cats, mems, apps, hide, (counts, last, since) ->
            resolve(cats, mems, apps, apps.filter { it.componentName.flattenToString() !in hide }, counts, last, since)
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Every category with what it holds now. Own categories keep their members (apps no longer on
     * the phone left out, apps sorted by name); smart ones are worked out from [apps], the launch
     * counts and times, and the install times.
     */
    fun resolve(
        categories: List<Category>,
        members: List<CategoryMember>,
        allApps: List<AppInfo>,
        apps: List<AppInfo>,
        launchCounts: Map<String, Int>,
        lastLaunches: Map<String, Long>,
        trackingSince: Long?,
        now: Long = System.currentTimeMillis()
    ): List<ResolvedCategory> {
        val byComponent = allApps.associateBy { it.componentName.flattenToString() }
        val byCategory = members.groupBy { it.categoryId }
        val ownAppMembers = categories.filter { it.smartRule == null }
            .flatMap { byCategory[it.id].orEmpty() }
            .filter { it.type == GridItemType.APP }
            .map { it.payload }.toSet()
        fun installOf(app: AppInfo) = installTime(ProfileComponents.real(app.componentName).packageName)
        fun asMembers(categoryId: Long, list: List<AppInfo>) = list.mapIndexed { index, app ->
            CategoryMember(categoryId = categoryId, type = GridItemType.APP, payload = app.componentName.flattenToString(), orderIndex = index)
        }
        return categories.map { category ->
            val resolved = when (category.smartRule) {
                null -> {
                    // Apps no longer on the phone are left out; links, places and the rest stay.
                    val own = byCategory[category.id].orEmpty().filter { it.type != GridItemType.APP || it.payload in byComponent }
                    if (category.manualOrder) own.sortedWith(compareBy({ it.orderIndex }, { it.id }))
                    else {
                        val (appMembers, others) = own.partition { it.type == GridItemType.APP }
                        appMembers.sortedBy { byComponent[it.payload]?.label?.lowercase() } + others
                    }
                }
                CategoryRule.NEW -> {
                    val days = category.ruleParam?.toLongOrNull()?.coerceIn(1, 365) ?: NEW_DAYS
                    asMembers(category.id, apps.filter { installOf(it) >= now - days * DAY }.sortedByDescending { installOf(it) })
                }
                CategoryRule.UNUSED -> {
                    val days = category.ruleParam?.toLongOrNull()?.coerceIn(1, 365) ?: UNUSED_DAYS
                    val since = trackingSince ?: now
                    asMembers(category.id, apps.filter { app ->
                        val key = app.componentName.flattenToString()
                        val last = lastLaunches[key] ?: maxOf(since, installOf(app))
                        last < now - days * DAY
                    })
                }
                CategoryRule.FREQUENT -> {
                    val most = category.ruleParam?.toIntOrNull()?.coerceIn(1, 60) ?: FREQUENT_MAX
                    asMembers(category.id, apps
                        .filter { (launchCounts[it.componentName.flattenToString()] ?: 0) >= FREQUENT_MIN_LAUNCHES }
                        .sortedByDescending { launchCounts[it.componentName.flattenToString()] ?: 0 }
                        .take(most))
                }
                CategoryRule.UNCATEGORIZED -> asMembers(category.id, apps.filter { it.componentName.flattenToString() !in ownAppMembers })
                CategoryRule.NAME -> {
                    val words = category.ruleParam.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    asMembers(category.id, if (words.isEmpty()) emptyList() else apps.filter { app -> words.any { app.label.contains(it, ignoreCase = true) } })
                }
                CategoryRule.ANDROID_KIND -> {
                    val kind = category.ruleParam?.let { p -> AndroidAppKind.entries.firstOrNull { it.name == p } }
                    asMembers(category.id, if (kind == null) emptyList() else apps.filter { androidKindOf(ProfileComponents.real(it.componentName).packageName) == kind })
                }
                CategoryRule.PROFILE -> asMembers(category.id, apps.filter { ProfileComponents.isProfile(it.componentName) })
            }
            ResolvedCategory(category, resolved)
        }
    }

    companion object {
        private const val DAY = 24L * 60 * 60 * 1000
        const val NEW_DAYS = 7L
        const val UNUSED_DAYS = 30L
        private const val FREQUENT_MIN_LAUNCHES = 3
        private const val FREQUENT_MAX = 16

        fun ruleNameRes(rule: CategoryRule): Int = when (rule) {
            CategoryRule.NEW -> R.string.category_rule_new
            CategoryRule.UNUSED -> R.string.category_rule_unused
            CategoryRule.FREQUENT -> R.string.category_rule_frequent
            CategoryRule.UNCATEGORIZED -> R.string.category_rule_uncategorized
            CategoryRule.NAME -> R.string.category_rule_name
            CategoryRule.ANDROID_KIND -> R.string.category_rule_android_kind
            CategoryRule.PROFILE -> R.string.category_rule_profile
        }

        fun ruleIconKey(rule: CategoryRule): String = when (rule) {
            CategoryRule.NEW -> "new"
            CategoryRule.UNUSED -> "sleep"
            CategoryRule.FREQUENT -> "trending"
            CategoryRule.UNCATEGORIZED -> "inbox"
            CategoryRule.NAME -> "search"
            CategoryRule.ANDROID_KIND -> "apps"
            CategoryRule.PROFILE -> "work"
        }

        /** The four smart categories offered ready-made; the others are made with your own rule. */
        val READY_RULES = listOf(CategoryRule.NEW, CategoryRule.UNUSED, CategoryRule.FREQUENT, CategoryRule.UNCATEGORIZED)
    }
}
