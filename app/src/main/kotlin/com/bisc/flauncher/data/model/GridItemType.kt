package com.bisc.flauncher.data.model

/** Kind of content held by an apps-row entry or a folder item. [PAGE]'s payload is a [LauncherPage.id]
 *  — a page-link entry, usable in any apps row (including a row in pill/single-line mode, e.g. the
 *  dock) to open that page on tap. [CONTACT]'s payload is a [com.bisc.flauncher.data.model.PinnedContact.contactId]
 *  — pinning a contact into a row also ensures a [PinnedContact] row exists for it (see
 *  PageContentRepository.addContactEntry), the same cached-name/photo snapshot pattern already used
 *  elsewhere for contacts. */
enum class GridItemType { APP, SHORTCUT, MAP_TARGET, WEBSITE, WIDGET, SYSTEM_WIDGET, FOLDER, PAGE, CONTACT }
