"""Builds the files people download for Freiraum.

    python tools/build_extensions.py
        Packs every folder in designs/ into release/<id>.freiraum-design and every folder in
        wallpaper-packs/ into release/<id>.freiraum-wallpapers.

    python tools/build_extensions.py import-pack <folder> <id> "<Name>" "<Name DE>" <light|dark>
        Turns a folder of pictures into wallpaper-packs/<id>/ (pack.json plus WebP files).
        Picture names may carry a light_/dark_ prefix and are matched by role:
        homescreen/home, applist, textsearch/search, appdrawer/appgrid, lockscreen/lock,
        backup_1..5/extra_1..5 for further pages. Needs Pillow.
"""
import json
import pathlib
import re
import sys
import zipfile

REPO = pathlib.Path(__file__).resolve().parent.parent
RELEASE = REPO / "release"

ROLES = {
    "home": ("homescreen", "home"),
    "appList": ("applist", "app_list"),
    "search": ("textsearch", "search"),
    "appGrid": ("appdrawer", "appgrid", "app_grid"),
    "lock": ("lockscreen", "lock"),
}


def build():
    RELEASE.mkdir(exist_ok=True)
    for folder, ext, manifest in ((REPO / "designs", "freiraum-design", "design.json"),
                                  (REPO / "wallpaper-packs", "freiraum-wallpapers", "pack.json")):
        if not folder.is_dir():
            continue
        for d in sorted(p for p in folder.iterdir() if (p / manifest).is_file()):
            target = RELEASE / f"{d.name}.{ext}"
            with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as z:
                for f in sorted(d.rglob("*")):
                    if f.is_file():
                        z.write(f, f.relative_to(d).as_posix())
            print(f"{target.relative_to(REPO)}  {target.stat().st_size // 1024} KB")


def import_pack(src, pack_id, name, name_de, mode):
    from PIL import Image

    src = pathlib.Path(src)
    out = REPO / "wallpaper-packs" / pack_id
    out.mkdir(parents=True, exist_ok=True)
    wallpapers, extras = {}, {}
    for f in sorted(src.iterdir()):
        if f.suffix.lower() not in (".png", ".jpg", ".jpeg", ".webp"):
            continue
        stem = re.sub(r"^(light|dark)_", "", f.stem.lower())
        extra = re.fullmatch(r"(?:backup|extra)_(\d+)", stem)
        if extra:
            key, target = int(extra.group(1)), f"extra_{extra.group(1)}.webp"
        else:
            key = next((role for role, names in ROLES.items() if stem in names), None)
            if key is None:
                print(f"skipped {f.name}")
                continue
            target = f"{key.lower()}.webp"
        Image.open(f).convert("RGB").save(out / target, "WEBP", quality=86, method=6)
        (extras if extra else wallpapers)[key] = target
    pack = {
        "format": 1,
        "id": pack_id,
        "name": name,
        "name_de": name_de,
        "author": "BISC Lab",
        "version": "1.0",
        "dark": mode == "dark",
        "wallpapers": {role: wallpapers[role] for role in ROLES if role in wallpapers},
        "extras": [extras[k] for k in sorted(extras)],
    }
    (out / "pack.json").write_text(json.dumps(pack, indent=2) + "\n", encoding="utf-8")
    print(f"{out.relative_to(REPO)}: {len(wallpapers)} pictures, {len(extras)} extras")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "import-pack":
        import_pack(*sys.argv[2:7])
    else:
        build()
