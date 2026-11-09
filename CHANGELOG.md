# Changelog

## 0.12.5

- Text search keyboard motion planned as one: opening, the results and the rail stand where the
  keyboard will end from the first frame and only the keyboard with its strip slides up; closing,
  results and rail ride down with the keyboard frame by frame and the strip fades on the way, so
  nothing comes apart or moves on its own afterwards

## 0.12.4

- Text search: the strip over the keyboard is the keyboard's top edge now, riding up and down with it
  frame by frame instead of waiting in place ahead of it; the results end right on it the whole way,
  no flicker, no jump

## 0.12.3

- Text search: the strip over the keyboard is in place from the first frame, where the keyboard
  will end, and fades in together with the field, cards and rail; the keyboard slides up under it
  instead of the strip pushing in afterwards
- Text search behavior: Web search and AI app open their settings with a tap on their name in the
  table, like Favorites; the separate Web & AI row on top is gone. Search engines and AI apps have a
  page each, with their result card as a preview that stays on top

## 0.12.2

- Text search behavior laid out anew: Web & AI, then the rail switch in its own section, then the
  categories. What Search and Rail do sits in two small cards above the table, not inside it. The
  table is a card of its own with a tinted head and fixed columns divided by vertical and horizontal
  lines; a short how-to sits below it

## 0.12.1

- Text search behavior: the rail switch has its own section above the table, since it turns the
  whole rail off. The table follows the corners table: a short legend on top says what Search and
  Rail do, clear column heads, fine lines between the rows. The quick choices are gone
- Favorites settings have their own page, opened with a tap on the Favorites category in the table
  (or through the settings search)

- Category rail: the magnifier grows the symbols in place, centred on the rail, on a solid round
  ground instead of reaching out sideways half transparent; a little less strong so it stays on
  screen at the edge
- The name bubble and the magnifier ground share one look, by default the result cards' look, and
  have their own look page: Text search > Appearance > Rail bubble

- Going back to automatic looks the same everywhere and says what it does: a color set by hand (text,
  border, accent) shows a small "Automatic" pill right beside its color instead of an extra row. A
  part's whole look shows its state under the pinned preview ("Own look" or "Automatic, matching
  the page") with "Back to automatic" right there. The same on every look page, folders included;
  no more two identical reset rows on one page

- Corners mean the same everywhere now: 100 % is the same radius on every part (never more than
  fully round). Before, widgets, element boxes and the search button counted from their own size, so
  50 % was already fully round there and the same value looked different on each part. What is
  stored is converted once; widgets on the old slight default get the new one
- Widgets and result cards are clearly rounder by default (80 %), in Modern and Neon too; element
  boxes and folders medium (50 %). The widget corner setting uses the whole range again

- Text search look pages: the previews sit on the same light backdrop as on the other pages again
  instead of a black one

## 0.12.0

- Wallpaper packs: the pages beside Home show the same picture as Home instead of getting a picture of
  their own, as they belong together
- Neon Dark and Neon Light: element boxes and folders only softly rounded, so nothing inside is cut
  off; widgets and result cards slightly rounded

- Switching between light and dark with a design that has both versions (Modern, Neon) now switches to
  the other version of that design instead of resetting every surface to plain defaults; with System
  this happens on its own when the phone changes mode
- Cleanup: unused texts and code removed, a German text whose percent sign was read as a placeholder
  fixed, outdated arrow icons replaced; search engine presets follow a language change right away

- Modern Dark and Modern Light reworked down to every part: deep grey (dark) or white (light)
  surfaces, nearly opaque; blue accent borders on search fields, element boxes, folders, the A-Z and
  category rails, the search button and the letter circles; no borders on Dock, Panel, widgets and
  result cards. Dock, Panel, rails, search button and fields fully round; element boxes and folders
  only softly rounded so nothing inside is cut off; widgets and result cards slightly rounded. Result
  cards and the strip over the keyboard are set slightly apart from the surfaces
- Designs can now set widgets apart from element boxes and the corners of each part; widgets and
  element boxes added later take the active design's look instead of a fixed default

- Text search: the category rail is in place the moment the search opens, together with the field
  and cards, instead of building up late while the keyboard comes. It knows how much room the
  keyboard takes (learned the first time and kept up to date, also with the strip on top of the
  keyboard) and only the keyboard slides in below it
- Closing the keyboard: the rail glides calmly into the middle instead of jumping up and down

- Apps & Elements > Apps: a link to Icons on top (icon pack, shape and size for all apps), showing the
  active icon pack

- Corners of all parts: "Apps in folders" did nothing visible and is now "Folders", the corners
  of the opened folder card. They can also be set on the folder look page

- Text search category rail: the symbols are much easier to see. On a dark rail their colors are
  lifted towards white, and categories without results are only slightly faint instead of nearly
  invisible
- The Text search rail's corners are its own too, no longer the A-Z rail's (kept as they were)
- App grid search field and the Text search parts use the same look page with the preview pinned
  on top and shown on the page's real light or dark tone

- App list looks: A-Z rail, search button, letter circles and search field each get one page with a
  live preview that stays on top while scrolling. The preview sits on a backdrop as light or dark
  as the App list page really is, and "automatic" colors show as they will turn out there
- Search button, App list search field and the Text search category rail no longer follow the A-Z
  rail: each has its own look, or the automatic one until set. What they showed so far is kept
- Every look page (Dock, Panel, folders, Text search parts) keeps its preview in view while scrolling

- Text search category rail: dragging along it works like a magnifier. The symbol under the thumb and
  its neighbours grow, light up and lean towards the results, a ring in the category color follows
  the finger, and a soft tick marks each new category. The bubble now sits above the thumb and names
  the category next to its symbol

- Closing a folder is the opening backwards: it shrinks back towards where it was tapped while it
  fades, and the darkened, blurred home screen behind it clears at the same pace instead of
  snapping back

- Folders have fixed places now, like the Dock: every empty cell in the folder editor shows a +
  that adds something exactly there, so apps can sit with gaps in between to make your own pattern.
  Moving an app steps through the cells, empty ones too; removing one leaves its cell empty. When
  all cells are taken, a further row offers more. Existing folders keep their order
- Text search behavior: the states under each category show in full again (on two lines if needed)

- Text search behavior rebuilt: the categories as a table with two columns, Search (results with
  every query) and Rail (a place in the rail). Together they give four clear states, shown under
  each name: always there, results only without a rail place, on demand (results only after a tap
  in the rail) and off. The X and the separate Removed list are gone; off categories keep their
  place
- Quick choices above the table: everything on, apps first with the rest on demand, all on demand;
  the rail itself can be switched off right there, which greys out the Rail column
- Categories come right after Web & AI, the favorites below

- Web & AI: a symbol can also come from an installed icon pack (tap the symbol, then From icon pack);
  AI apps show their icon from the active icon pack on their own, like every other app
- Category symbols: the choice is always a full 6 x 6 grid (four new symbols: calendar, plane,
  fitness, games), and the category's own symbol no longer shows twice

## 0.11.2

- Text search: several search engines and several AI apps. Each still gets one result card: the
  first runs with a tap, the others sit below it as chips to pick instead
- New settings page Text search > Behavior > Web & AI: engines and AI apps in a list to drag into
  order, ready-made engines (DuckDuckGo, Startpage, Google, Brave, Ecosia, Bing, Qwant, Wikipedia)
  or your own with name and address, and a live preview of both cards
- Every engine and AI app can get a picture of your own as its symbol (tap the symbol); without one,
  engines show their initial on their color and AI apps their app icon. Backups take the pictures
  along
- The earlier single engine, own search address and AI app carry over

- Text search categories get their own symbol: tap a category in the behavior settings to pick one
  of 32 symbols and its color, with a large preview; the rail and the result cards show it
- Text search behavior: long category names stay on one line and slide sideways to be read in full

- Text search: with the keyboard up, the category rail reaches from the field to the keyboard with
  the same gap at both ends, its top level with the first result card; with the keyboard down it
  sits in the middle as before
- Text search: tapping a picked category again shows all categories again (the rail kept an old
  state and did not notice the category was picked)

- Folders rebuilt in the settings: all folders as tiles with their icon and what is inside, a tile
  to make a new one, and the look of all folders above
- Making a folder shows it while picking: the name and the picked apps in a small folder card, the
  apps to pick as an icon grid with a tick; afterwards it opens straight away to arrange it
- Arranging a folder happens on the folder itself, live: its icon and name side by side, the folder
  as it opens with widgets at their height and the real apps in their rows. Tap a part to change it
  right below (height, position, all settings, remove), two large buttons add an app or element or a
  widget, and each row shows as a line of boxes with - and +

## 0.11.1

- Page previews in the settings are true to scale: every row at its real height and gap, element
  boxes and the Dock with their real app icons, Freiraum widgets with their symbol (the clock shows
  a time), Android widgets with their app icon and name
- Text search, App list and App grid open with a short introduction and two large cards,
  Appearance (how it looks) and Behavior (what it does), each listing what is inside
- Text search, Behavior: the categories stay on one line each
- Tips moved back to System

- Folder settings: widgets and apps are two clearly named parts, Widgets at the top and Apps and
  elements below, each with a short explanation. Every widget is a card with a block as tall as the
  widget, its height with - and + right there, moving and removing; a tap opens all its settings.
  A widget can always be made smaller again, even when the folder is full

- Text search: the field starts white and round like the App grid's, and reaches from the cards'
  edge to the rail's edge; the category rail starts with the look of the A-Z rail and sits as far
  from the cards as from the screen edge. Rail and field are set apart from each other
- Text search: tapping a category no longer leaves the search narrowed to it after typing on or
  scrolling, so contacts, addresses and the rest show again
- Fixed a crash on start in the debug build (the home screen had grown too large to load)

## 0.11.0

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
- Opening the search is one smooth motion: a gentle zoom, the background fades in, the field slides
  in (from the top, or from below with the keyboard), the rail from its edge and the cards one after
  another, while the keyboard comes up at the same time and everything follows it frame by frame.
  Closing takes the keyboard along right away. Without animations everything is there at once

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
