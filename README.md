# Moon Menu (Fabric, Minecraft 26.2)

A glass-style, transparent mod menu that lists all your installed mods automatically.
Needs Java 25, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.2.

## Get the jar
Push this folder to GitHub (include the hidden `.github` folder), open the Actions tab,
download the `glassmenu-jar` artifact. Or locally: JDK 25, `gradle wrapper && ./gradlew build`
-> `build/libs/glassmenu-1.0.0.jar`. Put it in `mods` with Fabric API.

## Use it
Right Shift opens the menu (rebind under Controls > Moon Menu). Esc / Right Shift / click outside closes it.

- **Installed Mods tab:** lists every mod in your `mods` folder automatically (name, version, description).
  Drop a new mod in, restart the game, and it shows up. No setup needed.
- **Modules tab:** click a row to toggle it, then click the `>` arrow to adjust its settings. Every module has its own accent hue and background opacity controls; modules may also have extra options.
- **Edit HUD tab:** drag each HUD to move it, scroll over it to resize, or right-click it to reset. The health module includes a resizable health bar.
- **Theme button:** switch between dark and light mode. Your theme and module settings are saved to `config/glassmenu.json`.

Built-in modules include FPS, CPS, Coordinates, Armor Display, Sprint, Health, Hunger, Target HUD, Attack Cooldown, PvP Supplies, Status Effects, Clock, Speed, Direction, Keys, Memory, and Session.

## Add a module (2 steps)
1. Create a class:
```java
public class WatermarkModule extends Module {
    private final NumberSetting size = number("size", "Size", 1, 5, 1, 3); // optional slider
    public WatermarkModule() { super("watermark", "Watermark", "Shows a label.", false); }
    @Override public String hudLine(Minecraft mc) { return "Moon Menu"; }
}
```
2. Register it in `GlassMenu.java`: `ModuleManager.register(new WatermarkModule());`

Hooks you can override: `onEnable`, `onDisable`, `onTick`, `onFrame`, `hudLine`, `renderHud`.
Other mods can add modules too: declare a `"glassmenu"` entrypoint in their fabric.mod.json
pointing at a class that implements `ModuleProvider`.
