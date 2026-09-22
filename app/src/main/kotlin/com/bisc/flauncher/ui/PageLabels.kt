package com.bisc.flauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherPage

/** Display name of a page for lists and titles. */
@Composable
fun pageLabel(page: LauncherPage, pages: List<LauncherPage>): String = when {
    page.isHome -> stringResource(R.string.settings_pages_home)
    page.isAppOverview -> stringResource(R.string.settings_pages_app_overview)
    else -> {
        val number = pages.filterNot { it.isHome || it.isAppOverview }.indexOf(page) + 1
        stringResource(R.string.settings_pages_page, number)
    }
}
