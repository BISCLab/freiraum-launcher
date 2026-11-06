# Changelog

## Unreleased

- Animations setting built like the status bar one: one switch for all animations, below it each
  animation on its own; the in-between level Subtle is gone

## 0.9.1

- App list: the letter bubble of the A-Z index can be pulled into the list like a spring - the
  further, the harder - glides after the finger and bounces back when let go
- Designs keep Freiraum's corner rule (widgets only slightly rounded, everything else by the general
  roundness) instead of one value for all
- The Neon design is now Neon Light: light, with its own wallpapers and dark text and icons

## 0.9.0

- Designs: ready-made looks with colors, borders, wallpapers and effects, under Design, Designs.
  Download a .freiraum-design file in the browser and add it; preview, apply, remove, go back to
  your own look, or save your current look as a design. Pages, apps and folders stay untouched
- First design: Neon, dark with glowing borders, a fine grid and soft light spots; the glow can be
  still, run once when shown, or keep running round
- Ripples on the home screen: a ring for a tap, two for a double tap, a soft wave for a long press
- Every animation can be switched on or off on its own under Design, Animations
- If Android's Remove animations is on, the Animations setting says so and links to it
- New animations: apps open by zooming out of the tapped icon; app list, App grid, Text search and
  folders grow out of where you tapped and shrink back; pages sink back a little while swiping;
  settings pages glide in and out; on Android 14+ the back swipe shrinks the screen with your finger.
  Choose Full, Subtle or Off under Design, Animations; Android's Remove animations turns them off

## 0.8.0

- New short welcome walkthrough on first start: what Freiraum is, its gestures, where the settings
  are, the permissions and the lock screen wallpaper; skippable at any point
- Weather widget: always exactly six whole hours and three whole days side by side, no cut-off tile
  at the edge, and swiping snaps to whole tiles
- Widgets with several pages (weather hours and days, notes) can be moved with buttons instead of
  swiping, which avoids clashes with swiping between pages. The buttons have their own symbol
  (chevron, triangle, arrow), fill, border, corners and opacity; set it under Design, Paging in widgets

## 0.7.0

- Tablets (beta, still experimental): Text search, settings, app list, Dock and Panel stay at a
  comfortable width in the middle instead of stretching across the screen, and the App grid adds
  columns on wide screens; the settings show a note that tablet support is in beta
- The Home button now also leaves the launcher's own settings and returns to the home screen
- New setting under System: lock the launcher to portrait so it doesn't turn with the device

## 0.6.0

- New installs start with a finished look: bundled wallpapers for Home, app list and Text search,
  light theme, rounded font, swipe up for the app list, double tap for Text search, long press for
  the App grid, and a matching search and app list design
- On first start, Freiraum offers to put a matching wallpaper on the lock screen
- The language setting shows only the chosen language; tap it to pick another
- The endless carousel setting notes that it can make swiping between pages less smooth
- Folders no longer scroll or get cut off: they grow up to nearly the whole screen, and widgets
  inside a folder can be scrolled
- A widget in a folder can only be made as tall as still fits on the screen
- The widget height sheet in folders keeps its Save button reachable with tall widgets

## 0.5.0

- Text search: the search field has the new look of the App grid's field; the Home button and the
  lines in the search bar are gone (back gesture closes the search)
- Text search: the search bar can sit at the bottom, right on the keyboard, with the categories and
  the keyboard strip above the field
- Text search: the category symbols in the search bar can be hidden
- Text search: the search field can float on its own, without the box around it, at the top or bottom
- The search field's fill, border and corners can be customized in Text search, in the app list (own search) and in the App grid
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
