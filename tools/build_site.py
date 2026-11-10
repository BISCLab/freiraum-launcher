"""Builds the extensions page for GitHub Pages: every design, wallpaper pack and widget source on one
page, with the files to download next to it.

    python tools/build_site.py [output folder, default _site]

Needs nothing beyond Python itself. The page is rebuilt and published by .github/workflows/pages.yml
on every push to master, so it never has to be committed.
"""
import base64
import html
import io
import json
import pathlib
import shutil
import sys
import zipfile

REPO = pathlib.Path(__file__).resolve().parent.parent
RELEASES = "https://github.com/BISCLab/freiraum-launcher/releases/latest"
SOURCE = "https://github.com/BISCLab/freiraum-launcher"
# A design travels inside its "open in Freiraum" link; the launcher refuses more than this.
MAX_LINK_BYTES = 300 * 1024


def both(de, en):
    """One text in both languages; the page shows the one that fits the browser."""
    return f'<span lang="de">{de}</span><span lang="en">{en}</span>'


def css_color(argb, alpha=1.0):
    """#AARRGGBB (or #RRGGBB) as CSS, its own alpha times [alpha]."""
    if not argb:
        return "transparent"
    v = argb.lstrip("#")
    if len(v) == 6:
        v = "FF" + v
    a, r, g, b = (int(v[i:i + 2], 16) for i in (0, 2, 4, 6))
    return f"rgba({r},{g},{b},{a / 255 * alpha:.2f})"


def zipped(folder):
    """The folder as the file people download, built the same way every time."""
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(folder.rglob("*")):
            if f.is_file():
                info = zipfile.ZipInfo(f.relative_to(folder).as_posix(), date_time=(2020, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                z.writestr(info, f.read_bytes())
    return buffer.getvalue()


def design_preview(d):
    """A small phone drawn from the design's own colors."""
    dark = d.get("theme") == "DARK"
    fx = d.get("effects") or {}
    accent = css_color(d.get("accent") or "#FF0758F5")
    ground = "#0c0d11" if dark else "#eef0f5"
    blobs = fx.get("blobColors") or []
    spots = ",".join(
        f"radial-gradient(circle at {x}% {y}%, {css_color(c, 0.55 * float(fx.get('blobStrength', 1)))} 0, transparent 55%)"
        for c, (x, y) in zip(blobs, ((18, 22), (85, 48), (30, 88)))
    ) if fx.get("blobs") else ""
    background = f"{spots + ',' if spots else ''}linear-gradient({ground},{ground})"
    glow = fx.get("glowColors") or []
    ring = f"linear-gradient(120deg,{','.join(css_color(c) for c in glow)})" if fx.get("glowBorder") and glow else None

    def surface(part, radius):
        p = d.get(part) or {}
        fill = css_color(p.get("fill"), float(p.get("opacity", 1))) if p.get("style", "COLOR") != "NONE" else "transparent"
        text = css_color(p.get("contentColor")) if p.get("contentColor") else ("#e8eaf0" if dark else "#1b1c20")
        if ring:
            border = f"border:1.5px solid transparent;background:linear-gradient({fill},{fill}) padding-box,{ring} border-box;background-color:{ground}"
        elif p.get("border"):
            border = f"border:1.5px solid {css_color(p.get('borderColor')) if p.get('borderColor') else accent};background:{fill}"
        else:
            border = f"background:{fill}"
        return f"{border};color:{text};border-radius:{radius}px"

    corner = int(d.get("cornerRadiusPercent", 100)) * 0.28
    widget_corner = int((d.get("corners") or {}).get("widgets", d.get("widgetCornerRadiusPercent", 80))) * 0.2
    dots = "".join(f'<i style="background:{accent if i == 0 else "currentColor"};opacity:{1 if i == 0 else 0.55}"></i>' for i in range(4))
    return (
        f'<div class="phone" style="background:{background}">'
        f'<div class="p-clock" style="color:{"#f2f3f7" if dark else "#16171b"}">9:41</div>'
        f'<div class="p-card" style="{surface("box", widget_corner)}"><b></b><b></b></div>'
        f'<div class="p-panel" style="{surface("panel", corner)}">{dots}</div>'
        f'<div class="p-dock" style="{surface("dock", corner)}">{dots}</div>'
        f'</div>'
    )


def design_card(d, data=None, bundled=False):
    name = both(html.escape(d.get("name_de") or d["name"]), html.escape(d["name"]))
    meta = f'{html.escape(d.get("author", ""))} · {html.escape(d.get("version", ""))}'
    if bundled:
        actions = f'<p class="note">{both("Im Launcher enthalten", "Included in the launcher")}</p>'
    else:
        file = f'files/{d["id"]}.freiraum-design'
        link = "freiraum://design#" + base64.urlsafe_b64encode(data).decode().rstrip("=")
        open_in = f'<a class="btn primary" href="{link}">{both("In Freiraum öffnen", "Open in Freiraum")}</a>' if len(data) <= MAX_LINK_BYTES else ""
        actions = f'<div class="actions">{open_in}<a class="btn" href="{file}" download>{both("Datei laden", "Download file")}</a></div>'
    return f'<article class="card">{design_preview(d)}<h3>{name}</h3><p class="meta">{meta}</p>{actions}</article>'


def pack_card(p, size):
    name = both(html.escape(p.get("name_de") or p["name"]), html.escape(p["name"]))
    pictures = [p["wallpapers"][r] for r in ("home", "appList", "search", "appGrid", "lock") if r in p.get("wallpapers", {})]
    strip = "".join(f'<img loading="lazy" src="packs/{p["id"]}/{html.escape(f)}" alt="">' for f in pictures[:4])
    count = len(pictures) + len(p.get("extras", []))
    meta = f'{html.escape(p.get("author", ""))} · {html.escape(p.get("version", ""))} · {count} {both("Bilder", "pictures")} · {size / 1024 / 1024:.1f} MB'
    file = f'files/{p["id"]}.freiraum-wallpapers'
    return (
        f'<article class="card"><div class="strip">{strip}</div><h3>{name}</h3><p class="meta">{meta}</p>'
        f'<div class="actions"><a class="btn primary" href="{file}" download>{both("Datei laden", "Download file")}</a></div></article>'
    )


STYLE = """
:root{--bg:#f4f5f9;--card:#fff;--text:#16171b;--soft:#5d6068;--line:#e2e4ea;--accent:#0758F5;--on-accent:#fff}
@media(prefers-color-scheme:dark){:root{--bg:#0e0f13;--card:#1c1c1e;--text:#eef0f5;--soft:#a3a6ae;--line:#2c2d33;--accent:#5b93ff;--on-accent:#0e0f13}}
*{box-sizing:border-box}
html[data-lang=de] [lang=en],html[data-lang=en] [lang=de]{display:none}
body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
main{max-width:1040px;margin:0 auto;padding:0 16px 56px}
header{max-width:1040px;margin:0 auto;padding:28px 16px 8px;display:flex;gap:16px;align-items:center}
header img{width:56px;height:56px;border-radius:16px}
header h1{font-size:1.45rem;margin:0;line-height:1.2}
header p{margin:2px 0 0;color:var(--soft)}
.lang{margin-left:auto;background:none;border:1px solid var(--line);color:var(--text);border-radius:999px;padding:6px 12px;font:inherit;cursor:pointer}
nav{position:sticky;top:0;background:var(--bg);z-index:2;display:flex;gap:8px;overflow-x:auto;padding:12px 16px;max-width:1040px;margin:0 auto}
nav a{flex:none;text-decoration:none;color:var(--text);border:1px solid var(--line);border-radius:999px;padding:7px 14px;background:var(--card)}
.how{background:var(--card);border-radius:24px;padding:18px 20px;margin:12px 0 4px}
.how h2{margin:0 0 6px;font-size:1.05rem}
.how ol{margin:0;padding-left:20px;color:var(--soft)}
section{padding-top:28px}
section>h2{font-size:1.25rem;margin:0 0 2px}
section>p{margin:0 0 14px;color:var(--soft)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:16px}
.card{background:var(--card);border-radius:24px;padding:16px;display:flex;flex-direction:column}
.card h3{margin:14px 0 0;font-size:1.05rem}
.meta{margin:2px 0 12px;color:var(--soft);font-size:.86rem}
.note{margin:auto 0 0;color:var(--soft);font-size:.9rem}
.actions{margin-top:auto;display:flex;gap:8px;flex-wrap:wrap}
.btn{flex:1;text-align:center;text-decoration:none;border-radius:999px;padding:10px 14px;border:1px solid var(--line);color:var(--text);white-space:nowrap}
.btn.primary{background:var(--accent);border-color:var(--accent);color:var(--on-accent);font-weight:600}
.phone{position:relative;aspect-ratio:9/14;border-radius:22px;overflow:hidden;border:1px solid var(--line);padding:12px;display:flex;flex-direction:column;gap:10px}
.p-clock{font-size:1.9rem;font-weight:300;line-height:1;margin:6px 2px 2px}
.p-card{padding:12px;display:flex;flex-direction:column;gap:7px}
.p-card b{display:block;height:7px;border-radius:4px;background:currentColor;opacity:.55}
.p-card b+b{width:60%;opacity:.3}
.p-panel,.p-dock{display:flex;justify-content:space-around;align-items:center;padding:9px 8px}
.p-dock{margin-top:auto}
.p-panel i,.p-dock i{width:20px;height:20px;border-radius:7px;display:block}
.strip{display:grid;grid-template-columns:repeat(4,1fr);gap:6px}
.strip img{width:100%;aspect-ratio:9/16;object-fit:cover;border-radius:12px;display:block;background:var(--line)}
.source{background:var(--card);border-radius:24px;padding:18px 20px}
.source h3{margin:0 0 4px}
.source ol{margin:8px 0 14px;padding-left:20px;color:var(--soft)}
footer{color:var(--soft);font-size:.9rem;text-align:center;padding:8px 16px 40px}
footer a,.source a.plain{color:var(--accent)}
"""

SCRIPT = """
const root=document.documentElement;
const saved=(()=>{try{return localStorage.getItem('lang')}catch(e){return null}})();
root.dataset.lang=saved||((navigator.language||'en').toLowerCase().startsWith('de')?'de':'en');
document.querySelector('.lang').addEventListener('click',()=>{
  root.dataset.lang=root.dataset.lang==='de'?'en':'de';
  try{localStorage.setItem('lang',root.dataset.lang)}catch(e){}
});
"""


def build(out):
    if out.exists():
        shutil.rmtree(out)
    (out / "files").mkdir(parents=True)

    cards = []
    for f in sorted((REPO / "app/src/main/assets/designs").glob("*.json")):
        cards.append(design_card(json.loads(f.read_text(encoding="utf-8")), bundled=True))
    for folder in sorted(p for p in (REPO / "designs").iterdir() if (p / "design.json").is_file()):
        d = json.loads((folder / "design.json").read_text(encoding="utf-8"))
        data = zipped(folder)
        (out / "files" / f'{d["id"]}.freiraum-design').write_bytes(data)
        cards.append(design_card(d, data))

    packs = []
    for folder in sorted(p for p in (REPO / "wallpaper-packs").iterdir() if (p / "pack.json").is_file()):
        p = json.loads((folder / "pack.json").read_text(encoding="utf-8"))
        data = zipped(folder)
        (out / "files" / f'{p["id"]}.freiraum-wallpapers').write_bytes(data)
        target = out / "packs" / p["id"]
        target.mkdir(parents=True)
        for name in list(p.get("wallpapers", {}).values())[:5]:
            shutil.copyfile(folder / name, target / name)
        packs.append(pack_card(p, len(data)))

    shutil.copyfile(REPO / "visuals/logo_flauncher.svg", out / "logo.svg")
    page = f"""<!doctype html>
<html lang="en" data-lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Freiraum Launcher: Extensions</title>
<meta name="description" content="Designs, wallpaper packs and widget sources for Freiraum Launcher.">
<link rel="icon" href="logo.svg">
<style>{STYLE}</style>
</head>
<body>
<header>
  <img src="logo.svg" alt="">
  <div><h1>Freiraum Launcher</h1><p>{both("Erweiterungen", "Extensions")}</p></div>
  <button class="lang" type="button"><span lang="de">English</span><span lang="en">Deutsch</span></button>
</header>
<nav>
  <a href="#designs">Designs</a>
  <a href="#packs">{both("Wallpaper-Packs", "Wallpaper packs")}</a>
  <a href="#widgets">Widgets</a>
  <a href="{RELEASES}">{both("Launcher laden", "Get the launcher")}</a>
</nav>
<main>
  <div class="how">
    <h2>{both("So kommt etwas in den Launcher", "How things get into the launcher")}</h2>
    <ol>
      <li>{both("Öffne diese Seite auf dem Handy, auf dem Freiraum läuft.", "Open this page on the phone that runs Freiraum.")}</li>
      <li>{both("Designs: „In Freiraum öffnen“ zeigt die Vorschau gleich im Launcher.", "Designs: “Open in Freiraum” shows the preview right in the launcher.")}</li>
      <li>{both("Dateien: laden, dann in der Dateien-App antippen und Freiraum wählen. Oder im Launcher: Einstellungen → Erweiterungen → hinzufügen.", "Files: download, then tap the file in the Files app and choose Freiraum. Or in the launcher: Settings → Extensions → add.")}</li>
    </ol>
  </div>

  <section id="designs">
    <h2>Designs</h2>
    <p>{both("Ein Design ändert nur das Aussehen: Farben, Rahmen, Ecken und Effekte. Seiten, Apps und Hintergründe bleiben.", "A design changes only the look: colors, borders, corners and effects. Pages, apps and backgrounds stay.")}</p>
    <div class="grid">{"".join(cards)}</div>
  </section>

  <section id="packs">
    <h2>{both("Wallpaper-Packs", "Wallpaper packs")}</h2>
    <p>{both("Ein Pack bringt für jede Seite ein Bild mit und passt zu jedem Design.", "A pack brings a picture for every page and goes with any design.")}</p>
    <div class="grid">{"".join(packs)}</div>
  </section>

  <section id="widgets">
    <h2>Widgets</h2>
    <p>{both("Uhr, Notizen und Musik sind im Launcher enthalten. Das Wetter braucht eine App, die die Daten bringt, denn Freiraum selbst hat keinen Internetzugriff.", "Clock, Notes and Music are included in the launcher. Weather needs an app that brings the data, because Freiraum itself has no internet access.")}</p>
    <div class="source">
      <h3>{both("Wetter mit Breezy Weather", "Weather with Breezy Weather")}</h3>
      <span class="meta">{both("Freie Wetter-App, viele Anbieter zur Auswahl", "Free weather app with many providers to choose from")}</span>
      <ol>
        <li>{both("Breezy Weather installieren und einen Ort einrichten.", "Install Breezy Weather and set up a location.")}</li>
        <li>{both("In Breezy Weather die Weitergabe der Wetterdaten für Gadgetbridge einschalten und dort Freiraum als Empfänger wählen.", "In Breezy Weather, switch on sharing weather data for Gadgetbridge and pick Freiraum as a receiver there.")}</li>
        <li>{both("In Freiraum eine Zeile mit dem Wetter-Widget hinzufügen.", "In Freiraum, add a row with the Weather widget.")}</li>
      </ol>
      <div class="actions"><a class="btn" href="https://f-droid.org/packages/org.breezyweather/">Breezy Weather ({both("F-Droid", "F-Droid")})</a></div>
    </div>
  </section>
</main>
<footer>
  {both("Freiraum Launcher ist freie Software.", "Freiraum Launcher is free software.")} <a href="{SOURCE}">{both("Quelltext auf GitHub", "Source on GitHub")}</a>
</footer>
<script>{SCRIPT}</script>
</body>
</html>
"""
    (out / "index.html").write_text(page, encoding="utf-8")
    print(f"{out}: {len(cards)} designs, {len(packs)} packs, {sum(f.stat().st_size for f in out.rglob('*') if f.is_file()) // 1024} KB")


if __name__ == "__main__":
    build(pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else REPO / "_site")
