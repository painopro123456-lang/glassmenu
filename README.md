# Moon Menu (Fabric, Minecraft 26.2)

A glass-style, transparent mod menu that lists all your installed mods automatically.
Needs Java 25, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.2.

## Download and install
**Direct mod download:** [Download GlassMenu.jar](https://github.com/painopro123456-lang/glassmenu/releases/download/latest/glassmenu-1.0.0.jar)

Download the `.jar` file above and put it directly into your Minecraft `mods` folder. Do not download the repository's **Source code (zip)** — that is the project source, not the mod.

The direct download is refreshed automatically when the `main` branch builds successfully. Requires Fabric Loader, Fabric API, and Java 25 for Minecraft 26.2. Version-tagged releases are also published under Releases.

To build locally: use JDK 25 and run `gradle --no-daemon build`; the mod JAR is created in `build/libs/`.

## Use it
Right Shift opens the menu (rebind under Controls > Moon Menu). Esc / Right Shift / click outside closes it.

- **Installed Mods tab:** lists every mod in your mods folder automatically (name, version, description).
  Drop a new mod in, restart the game, and it shows up. No setup needed.
- **Modules tab:** click a row to toggle it, then click the > arrow to adjust its settings. Every module has its own accent hue and background opacity controls; modules may also have extra options. Modules are grouped under Combat, Render, Player, and HUD headings. The new **Hit Health** module shows a short-lived health card above the last mob or player you attacked.
- **Edit HUD tab:** drag each HUD to move it, scroll over it to resize, or right-click it to reset. The health module includes a resizable health bar.
- **Module keybinds:** open Options > Controls > Key Binds > Moon Menu Modules to assign a key to each module. Zoom uses C by default; hold it to zoom. Minecraft saves your key assignments in `options.txt`.
- **Menu appearance:** use the color spectrum in the bottom-left to choose the menu accent, drag Opacity to adjust the glass panels, and click the theme button to switch between light and dark. These choices save to config/glassmenu.json.
- **Jumpscare:** covers the full screen above Moon Menu, shows the supplied picture, and plays a sharp scream when activated. Expand its settings to change visible time, fade time, opacity, and scream volume. To use another image, replace config/glassmenu/jumpscare.png with your own PNG (created on first use), then turn the module off and on. The bundled scream is “Scream” by DigitalDominic on Freesound, shared under CC0: https://freesound.org/people/DigitalDominic/sounds/175385/.

Built-in modules include FPS, CPS, Coordinates, Armor Display, Armor Status, Sprint, Health, Low Health Alert, Hunger, Target HUD, Target Effects, Hit Health, Attack Cooldown, PvP Supplies, Ping, Status Effects, Rainbow World, Jumpscare, Clock, Speed, Direction, Keys, Memory, Session, and Zoom. Rainbow World adds a client-side moving rainbow tint; expand its row to change the tint strength, color phase, and cycle speed. PvP additions include server ping, target potion effects, armor durability percentages, and a configurable low-health warning.

## Add a module (2 steps)
1. Create a class:
~~~java
public class WatermarkModule extends Module {
    private final NumberSetting size = number("size", "Size", 1, 5, 1, 3); // optional slider
    public WatermarkModule() { super("watermark", "Watermark", "Shows a label.", false); }
    @Override public String hudLine(Minecraft mc) { return "Moon Menu"; }
}
~~~
2. Register it in GlassMenu.java: ModuleManager.register(new WatermarkModule());

Hooks you can override: onEnable, onDisable, onTick, onFrame, hudLine, renderHud.
Other mods can add modules too: declare a "glassmenu" entrypoint in their fabric.mod.json
pointing at a class that implements ModuleProvider.
