# Changelog

## Unreleased

- Search categories can be removed entirely, so they are neither searched nor shown in the search bar; removed ones can be added back
- The search bar's category symbols always fit side by side across the whole bar, without scrolling

## 0.4.0

- New utility page "App grid": all apps in a grid (3 to 6 columns, icon size, app names on or off),
  with a search field at the bottom, top or off that filters the grid or opens Text search; chosen
  folders can be shown together at the top or sorted in among the apps. Opened by a gesture, an
  action button or a dock slot, like the other pages
- App names under icons are sharp now, without the blurred shadow
- New default page colors: Text search is a deep green, Home is brown
- App list: the search button can open its own app search right in the list instead of Text search
- Search categories can be reordered smoothly again; Folders no longer jumps back behind Apps
- The eyedropper button in color pickers is centered above "Reset to default"

## 0.3.0

- Text search:
  - Many more system settings (e.g. airplane mode, hotspot, notifications, dark theme, keyboard),
    found by what you look for ("brightness" finds Display); only those your phone offers are shown
  - New category "Freiraum settings" to jump straight into the launcher's own settings; it can be
    switched off like every other category. It replaces the separate e-mail command
  - Contacts are found from the start of a word of their name, by phone number in any format, and
    by their e-mail address, which they can now be e-mailed at
  - Shortcuts now cover every installed app's own shortcuts, not only pinned ones, with the app's name
  - Links are also found by their web address
  - Every category matches several words in any order (e.g. "klee 23") and ignores accents, umlauts
    and ß ("uber" finds "Über")
  - Places are found from the start of each word of their name or address
  - The search bar sits closer to the front camera (as much space below it as above), leaving more
    room for results; the lines above and below the field can each be switched off

## 0.2.0

First public release.
