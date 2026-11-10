"""Builds the extensions page for GitHub Pages: every design, wallpaper and widget on one
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
    mode = both("Dunkel", "Dark") if d.get("theme") == "DARK" else both("Hell", "Light")
    meta = f'{mode} · {html.escape(d.get("author", ""))} · v{html.escape(d.get("version", ""))}'
    if bundled:
        actions = f'<p class="included">{both("Im Launcher enthalten", "Included in the launcher")}</p>'
    else:
        file = f'files/{d["id"]}.freiraum-design'
        link = "freiraum://design#" + base64.urlsafe_b64encode(data).decode().rstrip("=")
        open_in = f'<a class="btn primary" href="{link}">{both("In Freiraum öffnen", "Open in Freiraum")}</a>' if len(data) <= MAX_LINK_BYTES else ""
        actions = f'<div class="actions">{open_in}<a class="btn" href="{file}" download>{both("Datei", "File")}</a></div>'
    return f'<article class="design">{design_preview(d)}<h3>{name}</h3><p class="meta">{meta}</p>{actions}</article>'


def pack_card(p, size):
    name = both(html.escape(p.get("name_de") or p["name"]), html.escape(p["name"]))
    roles = (("home", both("Startseite", "Home")), ("appList", both("App-Liste", "App list")), ("search", both("Textsuche", "Text search")),
             ("appGrid", both("App-Raster", "App grid")), ("lock", both("Sperrbildschirm", "Lock screen")))
    strip = "".join(
        f'<figure><img loading="lazy" src="packs/{p["id"]}/{html.escape(p["wallpapers"][role])}" alt=""><figcaption>{label}</figcaption></figure>'
        for role, label in roles if role in p.get("wallpapers", {})
    )
    count = len(p.get("wallpapers", {})) + len(p.get("extras", []))
    mode = both("Dunkel", "Dark") if p.get("dark") else both("Hell", "Light")
    meta = f'{mode} · {html.escape(p.get("author", ""))} · v{html.escape(p.get("version", ""))}'
    facts = f'{count} {both("Bilder", "pictures")} · {size / 1024 / 1024:.1f} MB'
    file = f'files/{p["id"]}.freiraum-wallpapers'
    return (
        f'<article class="wallpaper"><div class="w-info"><h3>{name}</h3><p class="meta">{meta}</p><p class="meta">{facts}</p>'
        f'<a class="btn primary" href="{file}" download>{both("Datei laden", "Download file")}</a></div>'
        f'<div class="w-strip">{strip}</div></article>'
    )


STYLE = """
@font-face{font-family:"Comfortaa";src:url(comfortaa.ttf);font-display:swap}
:root{--bg:#f6f5f1;--band:#eceae4;--paper:#fff;--text:#17181c;--soft:#5c5f66;--line:#d9d6ce;--blue:#0758F5;--on-blue:#fff}
@media(prefers-color-scheme:dark){:root{--bg:#101114;--band:#17181c;--paper:#1c1c1e;--text:#eceef3;--soft:#a1a4ac;--line:#2e3036}}
*{box-sizing:border-box}
html{scroll-behavior:smooth;scroll-padding-top:72px}
html[data-lang=de] [lang=en],html[data-lang=en] [lang=de]{display:none}
body{margin:0;background:var(--bg);color:var(--text);font:16px/1.55 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
h1,h2,h3,.brand{font-family:"Comfortaa",system-ui,sans-serif;letter-spacing:-.01em}
a{color:inherit}
.wrap{max-width:1080px;margin:0 auto;padding:0 20px}

.top{background:var(--blue);color:var(--on-blue)}
.bar{display:flex;align-items:center;gap:12px;padding:16px 0}
.bar img{width:40px;height:40px;border-radius:11px;border:1px solid rgba(255,255,255,.35)}
.brand{font-size:1.1rem;font-weight:700;white-space:nowrap}
.top .short{display:none}
.bar .space{flex:1}
.top .ghost{background:none;border:1px solid rgba(255,255,255,.55);color:inherit;border-radius:10px;padding:7px 12px;font:inherit;cursor:pointer;text-decoration:none;white-space:nowrap}
.hero{padding:36px 0 44px}
.hero h1{font-size:clamp(2rem,6vw,3.1rem);line-height:1.1;margin:0 0 12px}
.hero p{margin:0;max-width:34em;font-size:1.08rem;opacity:.92}

.jumpto{padding-top:30px}
.jumpto>p{margin:0 0 10px;font-size:.8rem;font-weight:600;letter-spacing:.09em;text-transform:uppercase;color:var(--soft)}
.jump{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}
.jump a{display:flex;align-items:center;gap:14px;text-decoration:none;background:var(--paper);border:1px solid var(--line);border-radius:16px;padding:14px 16px}
.jump .txt{flex:1;min-width:0}
.jump b{display:block;font-family:"Comfortaa",system-ui,sans-serif;font-size:1.08rem}
.jump span.what{display:block;color:var(--soft);font-size:.9rem;margin-top:1px}
.jump em{display:block;font-style:normal;color:var(--blue);font-size:.84rem;margin-top:6px;font-weight:600}
.jump .go{flex:none;width:34px;height:34px;border-radius:50%;background:var(--blue);color:#fff;display:grid;place-items:center;font-size:1.05rem;line-height:1}
@media(prefers-color-scheme:dark){.jump em{color:#7aa8ff}}

section{padding:76px 0}
#designs{padding-top:64px}
section.band{background:var(--band)}
.head{display:grid;grid-template-columns:1fr minmax(0,360px);gap:28px;align-items:end;margin-bottom:34px}
.head h2{font-size:clamp(1.6rem,4.5vw,2.2rem);margin:0 0 8px;padding-left:16px;border-left:5px solid var(--blue);line-height:1.15}
.head p{margin:0;color:var(--soft);max-width:36em}
.howto{border:1px solid var(--line);border-radius:14px;padding:14px 16px;background:var(--paper);font-size:.93rem}
.howto strong{display:block;margin-bottom:4px}
.howto ol{margin:0;padding-left:18px;color:var(--soft)}

.designs{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:34px 24px}
.design h3,.wallpaper h3{margin:14px 0 0;font-size:1.12rem}
.meta{margin:2px 0 0;color:var(--soft);font-size:.88rem}
.included{margin:12px 0 0;font-size:.9rem;color:var(--soft);border-top:1px solid var(--line);padding-top:10px}
.actions{margin-top:12px;display:flex;gap:8px}
.btn{display:inline-block;text-align:center;text-decoration:none;border-radius:10px;padding:9px 14px;border:1px solid var(--line);background:var(--paper);white-space:nowrap;font-weight:500}
.btn.primary{flex:1;background:var(--blue);border-color:var(--blue);color:#fff}

.phone{position:relative;aspect-ratio:9/15;border-radius:24px;overflow:hidden;border:1px solid var(--line);padding:14px;display:flex;flex-direction:column;gap:10px}
.p-clock{font-family:"Comfortaa",system-ui,sans-serif;font-size:2rem;line-height:1;margin:8px 2px 4px}
.p-card{padding:13px;display:flex;flex-direction:column;gap:7px}
.p-card b{display:block;height:7px;border-radius:4px;background:currentColor;opacity:.55}
.p-card b+b{width:60%;opacity:.3}
.p-panel,.p-dock{display:flex;justify-content:space-around;align-items:center;padding:9px 8px}
.p-dock{margin-top:auto}
.p-panel i,.p-dock i{width:20px;height:20px;border-radius:7px;display:block}

.wallpaper{display:grid;grid-template-columns:220px minmax(0,1fr);gap:28px;align-items:start;padding:28px 0;border-top:1px solid var(--line)}
.wallpaper:first-of-type{border-top:0;padding-top:0}
.w-info h3{margin-top:0}
.w-info .btn{margin-top:16px}
.w-strip{display:flex;gap:12px;overflow-x:auto;padding-bottom:6px;scroll-snap-type:x proximity}
.w-strip figure{margin:0;flex:0 0 132px;scroll-snap-align:start}
.w-strip img{width:100%;aspect-ratio:9/16;object-fit:cover;border-radius:16px;display:block;background:var(--line)}
.w-strip figcaption{font-size:.8rem;color:var(--soft);margin-top:6px;text-align:center}

.widgets{border:1px solid var(--line);border-radius:16px;background:var(--paper);overflow:hidden}
.widgets .row{display:grid;grid-template-columns:140px minmax(0,1fr) auto;gap:16px;padding:16px 18px;border-top:1px solid var(--line);align-items:center}
.widgets .row:first-child{border-top:0}
.widgets b{font-family:"Comfortaa",system-ui,sans-serif}
.widgets span.what{color:var(--soft)}
.tag{font-size:.82rem;border-radius:999px;padding:4px 10px;border:1px solid var(--line);white-space:nowrap}
.tag.needs{border-color:var(--blue);color:var(--blue)}
@media(prefers-color-scheme:dark){.tag.needs{border-color:#7aa8ff;color:#7aa8ff}}
.source{margin-top:28px;display:grid;grid-template-columns:minmax(0,1fr) auto;gap:20px;align-items:center;border-left:5px solid var(--blue);padding:4px 0 4px 18px}
.source h3{margin:0 0 6px;font-size:1.1rem}
.source ol{margin:0;padding-left:18px;color:var(--soft)}

footer{border-top:1px solid var(--line);padding:32px 0 48px;color:var(--soft);font-size:.92rem}
footer .cols{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:24px}
footer p{margin:0 0 8px;max-width:44em}
footer a{color:var(--text)}

@media(max-width:520px){.top .long{display:none}.top .short{display:inline}.brand{font-size:1rem}}
@media(max-width:760px){
  .jump{grid-template-columns:1fr;gap:8px}
  .jumpto{padding-top:22px}
  .hero{padding:22px 0 32px}
  section{padding:56px 0}
  .head{grid-template-columns:1fr;gap:18px;margin-bottom:26px}
  .designs{grid-template-columns:repeat(2,minmax(0,1fr));gap:28px 14px}
  .actions{flex-direction:column}
  .wallpaper{grid-template-columns:1fr;gap:16px}
  .widgets .row{grid-template-columns:1fr auto}
  .widgets span.what{grid-column:1/-1;grid-row:2}
  .source,footer .cols{grid-template-columns:1fr}
}
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


def count(n, de_one, de_many, en_one, en_many):
    return both(f"{n} {de_one if n == 1 else de_many}", f"{n} {en_one if n == 1 else en_many}")


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
        for name in p.get("wallpapers", {}).values():
            shutil.copyfile(folder / name, target / name)
        packs.append(pack_card(p, len(data)))

    shutil.copyfile(REPO / "visuals/logo_flauncher.svg", out / "logo.svg")
    shutil.copyfile(REPO / "app/src/main/res/font/comfortaa.ttf", out / "comfortaa.ttf")

    def widget(name, what, tag, needs=False):
        return f'<div class="row"><b>{name}</b><span class="what">{what}</span><span class="tag{" needs" if needs else ""}">{tag}</span></div>'

    included = both("Enthalten", "Included")
    page = f"""<!doctype html>
<html lang="en" data-lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Freiraum Launcher: Extensions</title>
<meta name="description" content="Designs, wallpapers and widgets for Freiraum Launcher.">
<link rel="icon" href="logo.svg">
<style>{STYLE}</style>
</head>
<body>
<div class="top">
  <div class="wrap">
    <div class="bar">
      <img src="logo.svg" alt="">
      <span class="brand">Freiraum Launcher</span>
      <span class="space"></span>
      <a class="ghost" href="{RELEASES}"><span class="long">{both("Launcher laden", "Get the launcher")}</span><span class="short">{both("Laden", "Get")}</span></a>
      <button class="ghost lang" type="button"><span lang="de">EN</span><span lang="en">DE</span></button>
    </div>
    <div class="hero">
      <h1>{both("Erweiterungen", "Extensions")}</h1>
      <p>{both("Alles, was du Freiraum hinzufügen kannst. Der Launcher selbst geht nie ins Internet, deshalb suchst du hier im Browser aus.", "Everything you can add to Freiraum. The launcher itself never goes online, so you pick things here in the browser.")}</p>
    </div>
  </div>
</div>

<div class="wrap jumpto">
  <p>{both("Direkt zu", "Jump to")}</p>
  <nav class="jump">
    <a href="#designs"><span class="txt"><b>Designs</b><span class="what">{both("Farben, Rahmen, Ecken und Effekte", "Colors, borders, corners and effects")}</span><em>{count(len(cards), "Design", "Designs", "design", "designs")}</em></span><span class="go" aria-hidden="true">&darr;</span></a>
    <a href="#wallpaper"><span class="txt"><b>Wallpaper</b><span class="what">{both("Ein Bild für jede Seite", "A picture for every page")}</span><em>{count(len(packs), "Wallpaper", "Wallpaper", "wallpaper", "wallpapers")}</em></span><span class="go" aria-hidden="true">&darr;</span></a>
    <a href="#widgets"><span class="txt"><b>Widgets</b><span class="what">{both("Uhr, Notizen, Musik, Wetter", "Clock, Notes, Music, Weather")}</span><em>{both("3 enthalten, 1 mit Quelle", "3 included, 1 with a source")}</em></span><span class="go" aria-hidden="true">&darr;</span></a>
  </nav>
</div>

<section id="designs">
  <div class="wrap">
    <div class="head">
      <div>
        <h2>Designs</h2>
        <p>{both("Ein Design ändert nur das Aussehen des Launchers. Deine Seiten, Apps und Hintergrundbilder bleiben, wie sie sind.", "A design changes only how the launcher looks. Your pages, apps and background pictures stay as they are.")}</p>
      </div>
      <div class="howto">
        <strong>{both("So fügst du ein Design hinzu", "How to add a design")}</strong>
        <ol>
          <li>{both("Diese Seite auf dem Handy mit Freiraum öffnen.", "Open this page on the phone that runs Freiraum.")}</li>
          <li>{both("„In Freiraum öffnen“ antippen, Vorschau ansehen, anwenden.", "Tap “Open in Freiraum”, look at the preview, apply.")}</li>
        </ol>
      </div>
    </div>
    <div class="designs">{"".join(cards)}</div>
  </div>
</section>

<section id="wallpaper" class="band">
  <span id="packs"></span>
  <div class="wrap">
    <div class="head">
      <div>
        <h2>Wallpaper</h2>
        <p>{both("Ein Wallpaper bringt für jede Seite ein passendes Bild mit, dazu eines für den Sperrbildschirm. Es passt zu jedem Design.", "A wallpaper brings a matching picture for every page, plus one for the lock screen. It goes with any design.")}</p>
      </div>
      <div class="howto">
        <strong>{both("So fügst du ein Wallpaper hinzu", "How to add a wallpaper")}</strong>
        <ol>
          <li>{both("Datei laden.", "Download the file.")}</li>
          <li>{both("In der Dateien-App antippen und Freiraum wählen. Oder im Launcher: Einstellungen → Erweiterungen → Wallpaper → hinzufügen.", "Tap it in the Files app and choose Freiraum. Or in the launcher: Settings → Extensions → Wallpapers → add.")}</li>
        </ol>
      </div>
    </div>
    {"".join(packs)}
  </div>
</section>

<section id="widgets">
  <div class="wrap">
    <div class="head">
      <div>
        <h2>Widgets</h2>
        <p>{both("Widgets legst du im Launcher als Zeile auf eine Seite. Drei sind immer dabei. Das Wetter braucht eine App, die die Daten bringt.", "You place widgets on a page as a row in the launcher. Three are always included. Weather needs an app that brings the data.")}</p>
      </div>
    </div>
    <div class="widgets">
      {widget(both("Uhr", "Clock"), both("Zeit, Datum, nächster Wecker", "Time, date, next alarm"), included)}
      {widget(both("Notizen", "Notes"), both("Notizen und Listen direkt auf der Seite", "Notes and lists right on the page"), included)}
      {widget(both("Musik", "Music"), both("Steuert, was gerade läuft", "Controls what is playing"), included)}
      {widget(both("Wetter", "Weather"), both("Aktuell, stündlich und die nächsten Tage", "Now, hourly and the next days"), both("Braucht eine Wetter-App", "Needs a weather app"), needs=True)}
    </div>
    <div class="source">
      <div>
        <h3>{both("Wetter einrichten mit Breezy Weather", "Set up Weather with Breezy Weather")}</h3>
        <ol>
          <li>{both("Breezy Weather installieren und einen Ort einrichten.", "Install Breezy Weather and set up a location.")}</li>
          <li>{both("Dort die Weitergabe der Wetterdaten für Gadgetbridge einschalten und Freiraum als Empfänger wählen.", "There, switch on sharing weather data for Gadgetbridge and pick Freiraum as a receiver.")}</li>
          <li>{both("In Freiraum eine Zeile mit dem Wetter-Widget hinzufügen.", "In Freiraum, add a row with the Weather widget.")}</li>
        </ol>
      </div>
      <a class="btn" href="https://f-droid.org/packages/org.breezyweather/">{both("Breezy Weather bei F-Droid", "Breezy Weather on F-Droid")}</a>
    </div>
  </div>
</section>

<footer>
  <div class="wrap cols">
    <div>
      <p><strong>{both("Sicherheit", "Safety")}</strong></p>
      <p>{both("Öffne nur Designs und Wallpaper aus Quellen, denen du traust. Der Launcher zeigt immer zuerst eine Vorschau und wendet nichts von selbst an. Ein Design ändert nur das Aussehen, ein Wallpaper nur die Hintergrundbilder.", "Open only designs and wallpapers from sources you trust. The launcher always shows a preview first and applies nothing by itself. A design changes only the look, a wallpaper only the background pictures.")}</p>
    </div>
    <div>
      <p><a href="{RELEASES}">{both("Launcher laden", "Get the launcher")}</a></p>
      <p><a href="{SOURCE}">{both("Quelltext auf GitHub", "Source on GitHub")}</a></p>
    </div>
  </div>
</footer>
<script>{SCRIPT}</script>
</body>
</html>
"""
    (out / "index.html").write_text(page, encoding="utf-8")
    print(f"{out}: {len(cards)} designs, {len(packs)} wallpapers, {sum(f.stat().st_size for f in out.rglob('*') if f.is_file()) // 1024} KB")


if __name__ == "__main__":
    build(pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else REPO / "_site")
