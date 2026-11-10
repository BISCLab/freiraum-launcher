# Freiraum wallpaper packs

A wallpaper pack brings a background picture for each page of Freiraum Launcher. Packs go with
every design.

## Install a pack

1. Open the [extensions page](https://bisclab.github.io/freiraum-launcher/#packs) on your phone
   and download a pack. The files are also on the
   [releases page](https://github.com/BISCLab/freiraum-launcher/releases).
2. Tap the downloaded file in the Files app and choose Freiraum. Or in Freiraum: Settings →
   Extensions → Wallpaper packs → Add wallpaper pack, and pick the file.
3. Tap the pack to see its pictures, then Apply. It can also set the lock screen.

"Without pictures" gives every page a plain background instead: matte black in dark mode, chalk
white in light mode.

## Packs

| Pack | Pictures |
|---|---|
| [Modern Light](modern-light/pack.json) | Home, App list, Text search, App grid, lock screen and 5 more pages |
| [Modern Dark](modern-dark/pack.json) | The same, dark |

## Make your own

A pack file is a ZIP with `pack.json` and the pictures (png, jpg or webp) side by side:

```json
{
  "format": 1,
  "id": "my-pack",
  "name": "My pack",
  "name_de": "Mein Pack",
  "author": "You",
  "version": "1.0",
  "dark": false,
  "wallpapers": { "home": "home.webp", "appList": "applist.webp", "search": "search.webp",
                  "appGrid": "appgrid.webp", "lock": "lock.webp" },
  "extras": ["extra_1.webp", "extra_2.webp"]
}
```

Every picture is optional. Pages without their own picture show the Home picture. `extras` go onto
further pages in their order.

`python tools/build_extensions.py import-pack <folder> <id> "<Name>" "<Name DE>" <light|dark>`
turns a folder of pictures into a pack folder here (names like `homescreen`, `applist`,
`textsearch`, `appdrawer`, `lockscreen`, `backup_1`…), and `python tools/build_extensions.py`
builds `release/<id>.freiraum-wallpapers`.
