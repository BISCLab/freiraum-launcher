package com.bisc.flauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind

/** Display name of a page for lists and titles. A custom [LauncherPage.name] always wins. */
@Composable
fun pageLabel(page: LauncherPage, pages: List<LauncherPage>): String = page.name?.takeIf { it.isNotBlank() } ?: when {
    page.isHome -> stringResource(R.string.settings_pages_home)
    page.isAppOverview -> stringResource(R.string.settings_pages_app_overview)
    page.kind == LauncherPageKind.SEARCH -> stringResource(R.string.settings_pages_search)
    page.kind == LauncherPageKind.APP_GRID -> stringResource(R.string.settings_pages_app_grid)
    else -> {
        val number = pages.filter { it.kind == LauncherPageKind.ROWS && !it.isHome }.indexOf(page) + 1
        stringResource(R.string.settings_pages_page, number)
    }
}
