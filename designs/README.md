# Freiraum designs

A design changes only the look of Freiraum Launcher: colors, borders, corners and effects. Pages,
apps, folders, notes and backgrounds stay as they are. Backgrounds come with
[wallpaper packs](../wallpaper-packs/README.md), so any design goes with any pack. A design holds
no personal data and no pictures, so it is safe to share.

## Install a design

1. Download a `.freiraum-design` file from the
   [releases page](https://github.com/BISCLab/freiraum-launcher/releases).
2. In Freiraum: Settings → Designs → Add design, and pick the file.
3. Tap the design to preview it, then Apply. "Back to my own look" returns to how it was before.

Freiraum has no internet access, so designs are always downloaded in the browser.

## Designs

| Design | Look |
|---|---|
| Modern Light, Modern Dark | Included in the app: Freiraum's own look, light or dark |
| [Neon Light](neon-light/design.json) | Light, with glowing borders in green, blue, violet and pink and soft light spots |
| [Neon Dark](neon-dark/design.json) | The same glow on matte black |

## Make your own

The easiest start: Settings → Designs → Save current look as design. That writes a
`.freiraum-design` file with your current colors, borders and corners.

A design file is a ZIP with `design.json` at the top level. `python tools/build_extensions.py`
packs every folder here into `release/<id>.freiraum-design`.

`design.json` (format 1), all fields optional except `format` and `id`:

| Field | Meaning |
|---|---|
| `id`, `name`, `name_de`, `author`, `version` | Identity |
| `theme` | `LIGHT` or `DARK` |
| `accent` | Accent color, `#AARRGGBB` |
| `font` | `SYSTEM`, `SERIF`, `MONOSPACE` or `ROUNDED` |
| `cornerRadiusPercent` | Roundness of everything except widgets, 0 (square) to 100 (fully round) |
| `widgetCornerRadiusPercent` | Roundness of widgets (Freiraum's default is 10) |
| `box`, `dock`, `panel` | Look of widgets and element boxes, the Dock, the Panel |
| `elements` | Look of single parts: `search_bar`, `search_field`, `search_results`, `search_appearance`, `folder_card`, `app_list_search_button`, `app_list_search_field`, `search_alphabet_rail`, `search_header_letter`, `app_grid_search_bar`, `widget_arrow` |
| `keyboardCapColor` | The strip above the keyboard |
| `effects` | See below |

A look (`box`, `dock`, … ) is `{"style": "COLOR" or "NONE", "fill": "#…", "fill2": "#…", "fillMode": "SOLID" or "GRADIENT", "opacity": 0–1, "border": true/false, "borderColor": "#…", "borderWidth": 0.5–6, "contentColor": "#…"}`.

`effects`:

| Field | Meaning |
|---|---|
| `glowBorder` | Glowing border around surfaces |
| `glowColors`, `baseColor` | Colors of the glowing stretch, and of the rest of the border |
| `motion`, `speedSeconds` | `STILL`, `ONCE` (one turn when shown) or `SLOW` (keeps running round) |
| `glowStrength` | 0–1 |
| `targets` | Where it glows: `dock`, `panel`, `search`, `folders`, `cards`, `widgets`, `buttons` |
| `grid`, `gridColor`, `gridSpacing` | Fine grid over the backgrounds |
| `blobs`, `blobColors`, `blobStrength` | Soft colored light spots over the backgrounds |

Older design files may still name `backgrounds` and pictures; Freiraum ignores them.
