# Changelog

## 0.15.0

- Tablet: how many rows fit on a page is always counted for portrait, so turning the tablet no longer changes it
- Tablet: in wide landscape the page shows its rows in two columns
- Widget size: the Save button stays in view and the sheet shows how many rows are still free on the page
- Home: adding to a full page now asks what to do instead of failing silently
- Page preview in the settings always shows the page in portrait
- Widget size opens in full, so the Save button is never below the screen edge
- Page preview on tablets shows rows and Dock as narrow and centered as on the real page
- Tablet: the App list uses the whole width, with up to four apps side by side; each letter stands beside its first line, and the A-Z rail and search button sit at the screen edge
- Tablet: the sections on the left of the settings carry their area colors, and the start page shows the extensions instead of a hint
- Tablet: the page tiles in Pages stay small enough on a low screen to leave room for the rest
- Tablet: in wide landscape the text search shows its cards two side by side, so more fits above the keyboard
- Page preview: empty space shows as a dashed outline, so it is no longer taken for a widget
- Editors with nothing unsaved show "All saved" with a check instead of a greyed-out Save button, so a new folder no longer looks as if it still had to be saved
- Tablets are no longer marked as beta in the settings

## 0.14.5

- Status bar settings as a list of switches, like the animations: "Hide everywhere" on top, then one
  switch per page, the text search and folders; on means hidden there
- With its animation off, the logo on the settings' start page stands still as a circle
- Picture tiles: the thin frame of the tiles not chosen is whole in the corners too; it lies outside
  the tile's rounded clip now
- The App list's search button is fully round at 100 %, and its preview has its real size
- Corners are picked with five presets (0, 25, 50, 75, 100 %), each showing its rounding, and a
  field for an own value, instead of a slider; every change shows in the preview at once
- Text search in the 3D designs: the search field, the category rail and the result cards fade in
  with their shadow from the first moment, instead of the shadow appearing at the end

## 0.14.4

- The logo on the settings' start page changes its shape faster and much more freely: square,
  rounded, round, flower, star, squircle, hexagon, cookie, clover, blob and triangle, melting from
  one into the next about every second and a half, for how freely the launcher can be changed
- The settings' start page has no back arrow any more; its title sits at the left, and the back
  gesture still leads home

## 0.14.3

- Quick access in the settings search shows nine flatter tiles, all above the keyboard, with the
  settings' start page first when the search was opened deeper in. It offers what most likely comes
  next from the page it was opened on: first what goes with that page (after Folders the Dock,
  the Panel and the Home screen; after Designs the wallpapers, colors and corners), then the pages
  furthest away by taps, then those opened often. Nothing leaves the phone
- Settings start page: the search field is gone again, the magnifier at the top is the way into the
  search. The heading is centered: the logo in the middle, slowly changing its shape between
  square, rounded and round, with the name and version under it. With motion off the logo stays
  square; the animation only runs while that page is open
- The picture tiles (background, icon shape, where an app shows) draw their frame on top of the
  picture, so a color or image no longer cuts into it at the rounded corners
- Animations has its own switch for the logo changing its shape on the settings' start page
- Area colors spread wider over the color wheel so none are mistaken for each other: Appearance
  magenta, Pages blue, Apps and elements green, System slate, Extensions orange. Each area also
  has its symbol, shown in the breadcrumbs where the area begins, for those who tell colors apart
  badly
- The parts of a page under its preview stand four in a line where there are four (App list)
- App list: letting go of the Aâ€“Z rail, the letter bubble fades out where it was, with its letter,
  instead of sliding empty to the top

## 0.14.2

- Background settings laid out anew, on every page's look (Home and other pages, App list, App
  grid, Text search): three equal small phones to choose from, Like all pages, Own image and Own
  color, each showing how it looks, the chosen one with an accent edge and a check. Below, only
  what fits the choice: for Like all pages how much it is darkened here and the way to change the
  background for all pages (no longer a separate row of its own), for an own image the image and
  its shade, for an own color the color. The background for all pages uses the same picture
  choice for image and color
- Fixed: taking a folder out of the Dock, the Panel or an Element-Box deleted the whole folder.
  Now it only leaves that place and stays under Folders with everything in it
- Fixed: something added to the Dock or the Panel without picking a slot could land behind the
  last slot, out of sight. It now takes the first free slot, and entries that already sat out of
  sight show up again in a free slot
- The folder page no longer has the switches to put a folder into the Dock or the Panel; folders
  are placed from the Dock and the Panel themselves
- Dock and Panel editor: the icons are sized to fit the preview, so all slots show and nothing is
  cut off. The bar under a picked element has light buttons, Settings and a red Remove
- Quick access moved from the settings start page into the settings search: a field under the
  header (or the magnifier) opens it, with the search on top and Quick access below. It shows six
  of the overview pages that are otherwise several steps away (Home screen, Dock, Panel, Folders,
  Gestures, Icons first), and adapts: the pages you open most move to the front, recent visits
  counting more than old ones
- The settings search is a page of its own, reached from every settings page by the magnifier at
  the top (pages with a draft to save leave it out), and from the field on the start page
- Folders: "Global folder settings" is now "Template for all folders", and says how many folders
  follow it ("2 of 3 follow it"); a folder with its own fill or border does not
- The Add window of Dock, Panel, folders and Element-Boxes: every tile of the same size, each named
  in a word (App, Folder, Shortcut, Place, Link, Page, Contact, Action button). "New folder" is gone
  there and in the folder choice; new folders are made under Folders, here an existing one is put
  in place
- The elements overview in the welcome tour has tiles of one size too
- Fixed: an app switched off for the text search still showed among the search favorites. Such an
  app (or one only found by its code) is now no favorite, is not counted for the favorites, cannot
  be picked as one, and its long-press menu shows "Add to favorites" greyed out
- Editing an app: the icon shape is three tiles (Like all apps, Rounded square, Circle), each with
  the app's own icon drawn as the launcher does; where it shows is three tiles (App list, App grid,
  Text search), shown with an accent edge and a check, hidden dimmed with a crossed-out eye,
  instead of switches whose meaning was unclear. The search code has its own row, only while the
  app is in the text search. Screen readers hear these tiles as choices and switches with their
  state
- Icons settings are fast: an icon pack is read once and kept, previews made once are reused, the
  preview shows the current icons at once instead of starting empty, and switching to another
  icon pack takes effect right away (its list is stored afterwards)
- Settings get area colors for orientation: Appearance violet, Pages blue, Apps and elements green,
  System slate, Extensions orange. Each page shows its area's color in titles, switches, checks
  and frames, and behind its symbols; the start page and the search show every tile in the color
  of where it leads, and the breadcrumbs mark each step with a dot of its color. Symbols are black
  or white for contrast. The colors are chosen to stay readable in light and dark, and an area is
  never told by color alone
- The accent color no longer tints the settings; it is for the launcher's own parts. While picking
  it, a frame in the chosen color shows it at once
- Better for screen readers: every button that shows only a symbol says what it does (Less, More,
  Clear input, Remove, Edit, move one place), an empty slot says "Add something here", and the
  switches in the text search's category table are switches with their category and state. The
  step buttons in the editors are 48 dp, big enough to hit easily
- Hints and second lines in the settings are a little darker (lighter in dark mode), so small
  text reads better, also in bright light
- Fixed: editing an app of the work profile or private space showed empty shape tiles
- Choosing an app for its shortcuts only offers apps of the own profile, as the shortcuts of an app
  in another profile cannot be reached from here
- Counts read right in the singular too, in English and German: "1 picture" / "1 Bild", "1 element",
  "1 of 3 follows it" / "1 von 3 folgt ihr"

## 0.14.1

- A single folder's look page works like every look page: the folder in small stays on top while
  the settings below scroll, with its real corners, and its state with the way back to the look of
  all folders right under it
- Work profile and private space everywhere: their apps (with the profile's badge) are in the text
  search, the App grid and every app list for adding, and can be put on pages, into the Dock, the
  Panel and folders, or onto a gesture, like any app. They always start in their profile; while
  the profile is paused or locked, a tap asks to switch it on. Their long-press menu opens App info
  in their profile; their own name, icon and size can be set like for any app
- The small page in the settings now uses the phone's exact measures: rows start below the real
  status bar, the Dock has its real height with the navigation bar under it, and the Panel's edge
  lies over the page instead of pushing it down. A full page now reaches down to one row gap above
  the Dock there too, like on the home screen

## 0.14.0

- Settings tiles never break a word: a name too long for its tile gets a little smaller instead
- Tiles laid out anew: every tile of a group has the same size, and symbol and name sit at the same height in all of them. Small tiles (three in a line) show only symbol, name and a short fact and are equally tall on every page.
- Apps and elements: back to five tiles of one size, two by two
- The browser link left the settings start page; Extensions has its own tiles now
- The App list is called App list everywhere, no more App overview on some pages
- App list, Text search and App grid show their page in miniature on their first settings page, like the Home screen; a tap on a part opens it
- Icons: the shape tiles and the preview row draw the icons exactly as the launcher does, so the
  rounded square shows as a rounded square, and they follow the chosen icon pack
- Backups are encrypted: exporting asks for a password (at least 8 characters, typed twice) and
  locks the file with it (AES-256-GCM, key from the password with PBKDF2). Importing a locked
  backup asks for the password and says so if it is wrong; older unlocked backups still import
- Element-Box, Dock and Panel are edited live, like a folder: the element sits on top in its own
  look and corners and stays in sight. Tap an element there to pick it, then move it, open its
  own settings or remove it in the bar below; a + adds something right there. The Element-Box
  shows its look changes at once, before saving. The rows of the Dock and the Panel are set with
  the same little boxes as in folders
- Work profile and private space: their apps show in the App list after your own, each under its
  own heading with its badge, and start in their profile. The heading pauses or turns on the work
  profile and locks or unlocks the private space; while it is off, a note says so. While searching
  the App list, matching apps of these profiles show too
- Text search finds more: a word of three letters or more is also found inside a word ("gram"
  finds Instagram), and from four letters on a small typo is forgiven ("chorme" finds Chrome).
  Matches at the start of a word still come first. Numbers stay exact. This works in every
  category, in the App list and App grid search and in the settings search
- Notification dots: an app with a notification gets a small dot in the accent color on its icon,
  on the pages, in the Dock and Panel, in folders, the App list and the App grid. Ongoing ones such
  as music do not count, and only which app has one is seen, never what it says. Switched on and
  off under Appearance > Icons, which also leads to the notification access if it is missing

## 0.13.8

- Panel and Dock open in one smooth motion: the card itself grows, with its shadow and edges at
  every step, instead of being uncovered first and getting its shadow at the end (3D designs)
- Text search behavior: a category name too long for its line fades out softly at the edge instead
  of being cut off hard; it still slides sideways to be read in full
- Settings start page: below the four areas, Extensions now has three tiles of its own (Designs,
  Wallpapers, Widgets, each with how many there are) that lead straight to them. Quick access
  shows Home screen, Icons and Gestures, and below them the three pages App list, Text search and
  App grid. The link to every extension in the browser sits at the bottom
- Settings search: the magnifier at the top of the settings searches every settings page by name
  and by what is on it, like the text search does
- More pages use tiles: Text search, App list and App grid (Appearance and Behavior, then their
  parts under the preview), Home screen, Panel and Dock, and in Appearance the color modes, the
  font, the status bar and the animations
- Also as tiles: in System the browser and maps app (each shows the chosen app) and backup
  (save, restore); in Apps and elements Apps and Folders side by side with Places, Links and
  Action buttons in a line below; in About the project links and the license texts
- The color mode for the settings is just called "Settings"; it still applies to the welcome tour
  too, as its tip says

## 0.13.7

- Choice windows (language, font, color modes, browser and maps app, what a gesture does) are built
  anew: a rounded window with a clear title, each option a piece of its own with a fine gap, the
  chosen one tinted with a check mark; apps show their icons. They open with a short fade
- Choices on the settings pages show the same check mark instead of a plain radio button
- Folders and Element-Boxes add their elements with the same tiles as the Dock and the Panel
- The settings search finds every page now, also the strip over the keyboard, Text search behavior,
  the App list's search button and A-Z rail, the App grid's grid, the menus, all folders and the
  arrows in widgets
- The "?" tips in the settings are on until switched off, so explanations are there from the start
- The app page opened from an app's menu has its name in the breadcrumbs
- Welcome tour: widgets can sit on a page, in the Panel or in a folder
- A page's rows are moved right in its small picture: tap a row to pick it, then move it up or down
  or open it; the list that repeated the picture is gone
- Tapping an element in the Dock's or Panel's content offers its own settings (app, folder, place,
  link, action button) besides taking it out
- System starts with Permissions and Language as tiles
- The Weather widget's settings lead to its data sources; every page's background leads to the
  background for all pages
- Text search behavior explains its table the first time, in a card that goes away once understood
- Dock and Panel are reached through one start page each, wherever they are opened from
- Settings start with four big tiles for the areas, each saying what is inside, and a Quick access
  row of six tiles: Designs, Wallpapers, Icons, Gestures, Text search, Folders
- Apps and elements is a page of tiles that say how many of each there are, with a line on what
  elements are for
- Appearance shows ready looks, all parts and background and icons as tiles instead of rows
- Adding a page with + makes it at once and opens it, without a choice in between that only ever had
  one option
- Dock and Panel settings start with their look in small and large cards: what is in it comes first
  (with how many elements and widgets), then when the Dock shows, then the look. Elements are no
  longer hidden under the look
- A folder says right under its name whether it sits in the Dock and in the Panel, and a switch puts
  it there or takes it out; its content has a heading of its own
- Welcome tour: a new step explains elements (apps, folders, places, links, contacts, action
  buttons) and where they go
- New: the Panel can hold widgets, looking like the ones in a folder: clock, weather, music, notes or
  an Android widget. The Panel grows with them and scrolls when it is full; while a finger is on a
  widget the Panel holds still, so lists inside the widget scroll on their own. Add and edit them
  under Home > Layout > Panel > Content
- Widgets in a folder and in the Panel can sit between the rows of apps, not only on top: the arrows
  move a widget past its neighbours and past whole rows
- Adding an element to the Dock or the Panel shows tiles instead of a list, with New folder among
  them: it asks for a name and opens the new folder right away to fill it
- A new page beside Home starts empty instead of with copies of Home's widgets; it takes Home's
  background, and Home's Dock and Panel come along
- Every page beside Home can leave out the Dock or the Panel on its own (its settings, Dock and Panel)
- The page in small in the settings has the phone's real proportions, and its rows are stretched like
  on the phone, so a full page no longer looks as if a row were free

## 0.13.6

- New: an app can be hidden from the App grid on its own, next to App list and Text search
- Fixed: apps hidden from the App list were also missing from every app choice in the settings
  (clock actions, gestures, Dock, folders, element boxes and the like); there they show again

## 0.13.5

- Element-Boxes have their corner setting in their editor again
- Menus have corners of their own, on their look page under Appearance > Menus
- Corners of all parts lists every part with corners, with clear names and where it sits: now also
  the arrows in widgets, the strip over the keyboard and the menus. Search fields (all) no longer
  changes the general roundness of everything else
- Colors of all parts also lists the arrows in widgets and the menus
- An app's own icon size counts in the Dock too, but only smaller, never bigger than its place; the
  app page says so
- Welcome tour built anew: the welcome is a full screen in the launcher's blue with softly drifting
  light, the logo coming in and what Freiraum stands for. The look comes first: Design, Wallpaper
  and Shape share one phone on top that shows the home screen with the choice right away, so every
  tap visibly changes something. The progress is a slim bar of steps
- The look pages of the App list's search button, A-Z rail and search field and of the App grid's
  search field have their fine corner setting again; the two search fields have corners of their own
- Corners of all parts: in the small App list the search button is the round button at the bottom
  and the field on top counts as a search field; Search fields sets every search field at once
- An app's own icon size now also counts in the App list and the Text search

## 0.13.4

- Popup menus dim and blur the screen behind them, so the menu stands out clearly; the gaps between
  the entries are the same everywhere; the menu opens below the app, or above when there is no room
- New: Appearance > Menus sets the color of the popup menus; designs can set it too
- Design effects (glow, relief) also reach the letter circles of the App list, the letter bubble
  while scrubbing and the name bubble of the category rail, and the menus
- Neon and 3D designs updated for this (menu look, effects on letters and menus)
- Welcome tour: 3D Light and 3D Dark among the designs; designs and wallpapers always three side
  by side, more in the next line, and the page fades out at its edges when it scrolls; a note that the Weather widget is included and gets its data from Breezy Weather
  for now, with more sources in the works

## 0.13.3

- New design effect Relief: surfaces stand out from the page with a soft shadow below, a lit edge on
  top and a slight sheen. A design chooses which parts get it; nothing moves, so it costs no power
- New designs to download: 3D Light and 3D Dark, built on the relief
- Popup menus have rounded corners and their entries are separate pieces with a fine gap between
  them, like a group in the settings: the long-press menu of an app, of an entry or widget in a
  folder and of a note, and the menus of the Music and Notes widgets
- Extensions page in the browser: the three shortcuts to Designs, Wallpapers and Widgets sit below
  the blue header as their own row, marked as jumps; the header fits on narrow phones

## 0.13.2

- Welcome tour brought up to date: a new step shows the widgets (Clock, Notes, Music, and Weather
  with what it needs); designs and wallpapers to download open the extensions page in the browser
  instead of GitHub; the settings step mentions Extensions

## 0.13.1

- Info page laid out anew: the launcher's logo, name, version and description together on a card in
  its own blue; No internet, No tracking and No ads as three tiles; a new row What is new; the
  maker with its logo on its own at the end
- Tablets held sideways: the settings come in two parts. The sections (Appearance, Pages, Apps and
  elements, System, Extensions) stay on the left, the chosen page is on the right. A page with
  unsaved changes still asks before a section takes you away
- Tablets: Home's rows keep to the same centered column as the Dock and the Panel instead of
  stretching across the whole screen
- Tablets: every settings page keeps to the centered column, also the app list under Apps, the
  editor pages that open on top, and the Save button
- Tablets: the strip over the keyboard is as wide as the keyboard
- Wallpaper packs are simply called Wallpapers everywhere
- The extensions page in the browser has a new layout: one clearly separated section each for
  Designs, Wallpapers and Widgets, each with its own short how-to

## 0.13.0

- New: Extensions, one page in the settings for everything that can be added: designs, wallpaper
  packs and widgets, with what is there of each. It replaces the three rows on the first settings
  page. Widgets lists what is included (Clock, Notes, Music) and shows whether the Weather widget
  is receiving data
- New: an extensions page in the browser shows every design and wallpaper pack with a preview.
  "Find designs" and "Find wallpaper packs" open it instead of a folder on GitHub
- New: a design opens straight from that page ("Open in Freiraum"), and a downloaded design or
  wallpaper pack file can be opened with Freiraum from a file manager. Either way it is installed
  and its preview shown, ready to apply
- A design or pack that came in from outside says so in its preview, with a reminder to apply it only
  from a source you trust
- New: a long press on an app opens its menu, in the App list, the App grid, the Text search, the
  Dock, the Panel and Element-Boxes: the app's own shortcuts, add to or remove from favorites,
  Customize (name, icon, where it shows), App info and Uninstall. Uninstall hands over to Android,
  which asks before removing anything; for this the launcher now declares the permission to request
  an uninstall
- Settings, one word per thing: Links (no longer also websites and internet addresses), Places (no
  longer also addresses), rows, and Delete for what is gone afterwards (pages, designs, packs)
- Settings: a row that opens another page shows an arrow everywhere; rows that open a choice do not
- Home's settings start with the same two large cards as App list, App grid and Text search
- A page's look has the same three groups on every page: icon and name, background, status bar. On
  Home and the pages beside it the background no longer sits on a page of its own
- Saving is the same within a page: the Text search page applies its text color at once like its
  other settings, and the music widget's cover switch waits for Save like the rest of its editor
- Back arrows in the settings have a spoken name for screen readers
- Text search settings sorted by one rule: Behavior holds what shows and where, Appearance only how
  it looks. The search field's position, the keyboard opening by itself, the rail switch and the
  rail's side sit together under Behavior as Layout; they are gone from the look pages, and the
  rail switch is there only once. The keyboard page is now the strip over the keyboard alone
- Choices between two or three short options are made right in their row (search field top or
  bottom, rail left or right, the App grid's search field); the font opens a short list like the
  color modes, each font shown in its own typeface
- Settings rows show a pressed effect; rows that only say something no longer react to a tap
- Fixed: the first settings page scrolls, so nothing is cut off on small screens or with large text
- Fixed: the arrows between the breadcrumbs at the bottom of the settings were invisible
- Fixed: a search code for a hidden app works regardless of upper and lower case and of a space at
  the end; a code left empty no longer makes the app unfindable
- Fixed: a link with an address that cannot be opened no longer crashes; such addresses are refused
  when the link is made, and a link that fails to open says so
- Weather broadcasts are limited to 1 MB unpacked, so another app cannot crash the launcher with one

## 0.12.6

- Text search opens as one motion with the keyboard: it starts when the keyboard starts and runs on
  the keyboard's curve; field, cards (Favorites too) and rail fade in and come up together instead
  of one after another, and the page no longer zooms, so the strip sits exactly on the keyboard
- Closing the keyboard, the strip stays on it all the way down instead of fading out ahead of it

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
