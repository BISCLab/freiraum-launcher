# Changelog

## Unreleased

### Text search
- Rebuilt: only the search field stays, at the top or on the keyboard; the box with the category
  row is gone
- The categories run down a rail beside the results, left or right, always above the keyboard.
  Tapping one jumps to its results and greys out the others until you scroll; dragging along the
  rail runs through the categories like the A-Z rail, with a bubble
- The category rail has its own page with its own look (fill, border, corners), showing it and its
  edge; until changed it looks like the search field
- New category Favorites: picked by hand, the most opened apps, or both. They show as soon as the
  search opens while Favorites is switched on in the categories, otherwise via the star in the rail.
  A long press on an app in the search adds or removes it. Launch counts stay on the phone
- The keyboard opens with the search again for everyone (it can still be switched off and hidden)

### Settings
- Dock, Panel, folders, a single folder, search field, category rail and search results each have
  one page for their look instead of separate Fill and Border pages: a live preview on top, then
  fill, text color, opacity, border and corners, saved together; their other options sit right
  under the preview
- New page Appearance → Colors of all parts: the small screens in the real colors, a color set for
  everything (Light, Dark, Glass, Accent) and a table to give each part its own look
- Corners of all parts: exactly three screens fit side by side; the category rail is a part of its own
- Icons page redesigned: real apps as a live preview of shape, size and icon pack, the shape as
  picture tiles, icon packs as cards with three of their own icons
- App list and App grid appearance pages start with a miniature to tap, like Text search
- Status bar page: every page and folders as a small screen, tap one to switch it
- The same words everywhere: Appearance, element box, icon pack, A-Z rail, and Edge for left or right
- The path bar at the bottom shows names on every page, never a raw path with slashes

### Also
- App grid: scroll up and down or swipe pages sideways, with as many rows as fit or 3 to 8, and
  optional page dots
- Folder widgets: long press one in an open folder to edit or remove it; folder settings have a
  remove button per widget, with a question
- Welcome tour: the gestures can be set right there; design and wallpaper tiles fit three side by side
- Backups hold the whole launcher to move it or set it up again: besides the look, pages and
  personal things also the installed designs (with your saved own look) and wallpaper packs
- Designs: the category rail is a part of its own, the old search bar box is gone
  (Neon Light 1.5, Neon Dark 1.3)

## 0.10.2

- Corners of all parts: changes only apply with Save, and leaving with unsaved changes asks first
- The charging animation moved to Appearance → Animations, as its own group below the animations

## 0.10.1

- Appearance now holds everything about look and motion: color modes (launcher, and settings with
  the welcome tour) side by side, accent, font, background and icons, status bar and portrait lock,
  animations and the charging animation; System keeps permissions, language, default apps, tips
  and backup
- Icons moved back from Apps & elements to Appearance
- The paging buttons look is reached from each widget that uses buttons, no longer from Appearance
- Switching the launcher color mode no longer touches backgrounds; only the plain default color
  swaps between matte black and chalk white
- Designs: Back to my design is now Remove design, with a clearer explanation
- Welcome tour: designs and wallpaper packs side by side to swipe through; a design is always
  chosen (no more None), Modern in the tour mode to begin with
- Welcome tour: hint that your own pictures can be picked any time later
- Welcome tour, shape: five small screens (Home, Panel, App list, Text search, Folder) show every
  part with its corners live; one choice for all, and below it a table with each part on its own:
  Dock, Panel, widgets, element boxes, apps in folders, search button, A-Z rail, search bars and
  search results. It starts from what the design brought
- The App list search button and the A-Z rail have corners of their own now
- Welcome tour: after setting Freiraum as default launcher you are back at the step you left
- Appearance: Designs and Wallpaper packs at the top as a shortcut, a new page Corners of all parts
  (the same screens and table as in the welcome tour), and the tips switch moved here
- App list: the letter bubble stays a perfect circle when pulled
- Design effects: the glowing border now also reaches the App list search button and the A-Z rail,
  and designs can let surfaces glow softly from their border (surfaceGlow); Neon Light and Neon
  Dark use it

## 0.10.0

- Settings rebuilt: "Design" is now "Appearance" (launcher color mode, accent, font, background,
  paging buttons in widgets); "Elements" is now "Apps & elements" and holds the icons too
- System has a new Display group: status bar and animations each open their own page with a short
  summary in the list, portrait lock, and a color mode just for the settings and the welcome tour
- Language moved to System
- Hiding the status bar everywhere still asks first, showing it again no longer does
- No more general corner setting; widget corners go up to 50% with a hint about the content
- Exporting a backup asks you to confirm it holds personal data; exporting a design explains it
  holds only the look and leaves out all pictures
- App list: the A-Z bubble no longer draws a line to the letter rail
- New: Wallpaper packs under Extensions. A pack brings a picture for Home, App list, Text search,
  App grid, further pages and the lock screen; "Without pictures" gives matte black or chalk white.
  Modern Light and Modern Dark are the first packs
- Designs no longer carry pictures, so any design goes with any wallpaper pack; Modern Light and
  Modern Dark come with the app, Neon Light and the new Neon Dark are downloads
- No more wallpapers inside the app (it got about 600 KB smaller); a fresh install starts plain
- Plain light background is now chalk white instead of pure white
- Switching light/dark keeps your pictures and recolors only plain backgrounds
- New welcome tour: logo, language behind a flag, a light/dark switch for the tour and settings,
  and Quick setup (everything recommended at once) or Set up myself
- Set up myself goes through gestures and settings, then lets you choose a design, a wallpaper pack
  (or none) and a shape: like the design, Round, Soft or Square, optionally part by part (Dock,
  widgets, search button, Text search, folders); widgets always stay a little less round
- The lock screen step only shows when the chosen wallpaper pack has a picture for it
- Swipe or paging buttons is now chosen per widget (weather, notes) in the widget's own settings, on
  pages and in folders; Appearance keeps only the look of the buttons. If you had buttons switched
  on, every widget keeps them
- Gesture ripples are now a drop on water: from Android 13 the page underneath bends in two or three
  spreading crests that fade out (two drops for a double tap, a wide slow one for a long press);
  older phones get soft double rings that thin out as they spread

## 0.9.2

- Settings are grouped into Basics (Design, Pages, Elements, System) and Extensions (Designs, and
  Plugins as a preview of what's coming); Designs also turn up in the settings search
- Widgets in folders can be set up as fully as widgets on pages: size, fill, border, text color,
  corners, music sources and all clock options including its tap actions
- Applying a design no longer makes widgets fully round; going back to your own look restores each
  widget's corners (or Freiraum's widget default for older saved looks)
- Neon Light: a border line under the Text search bar, and no more checkered grid over the wallpapers
- Designs: every installed design has its own remove button, with a confirmation
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
