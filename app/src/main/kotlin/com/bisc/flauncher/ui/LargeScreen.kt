package com.bisc.flauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Widest the search, settings, app list and the Dock/Panel get; on phones it never applies. */
val MaxContentWidth = 600.dp

/** The phone width the App grid's column setting is meant for; wider screens get more columns. */
val PhoneReferenceWidth = 412.dp

/** Tablets and other big screens: the shorter side is at least 600dp. Their support is in beta. */
@Composable
fun isLargeScreen(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600
