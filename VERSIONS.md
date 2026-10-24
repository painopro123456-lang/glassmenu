# Minecraft version compatibility

GlassMenu's target range is **Minecraft Java Edition 1.8.9 through 26.2**, inclusive. Versions older than 1.8.9 and newer than 26.2 are outside the current target range unless the project later expands it.

This range is a compatibility goal, not a claim that every version is already implemented. A JAR compiled for one Minecraft version must not be assumed to work on another. Use separate version-specific builds and only advertise a version after it builds and has been tested in-game.

## Compatibility policy

- Target range: Minecraft Java 1.8.9–26.2, inclusive.
- Do not support versions older than 1.8.9.
- Keep the existing Minecraft 26.2 implementation as the modern baseline.
- Publish a separate JAR for each supported Minecraft/loader target.
- Isolate version-specific GUI, HUD, keybinding, rendering, and game API calls behind version-specific implementations where practical.
- Never mark a version as supported until its build succeeds and has been smoke-tested in that game version.

## Important loader limitation

The current project is a Fabric mod. Fabric officially supports releases from 1.14 onward, so **Minecraft 1.8.9 cannot be supported by merely changing the version number or Fabric metadata**. Supporting 1.8.9 requires a separate legacy implementation using a loader/toolchain compatible with that era (for example, Forge), or a separately maintained legacy branch. Fabric builds can cover the 1.14–26.2 part of the target range; 1.8.9 must be a distinct port.

## Planned rollout

| Tier | Target versions | Status |
|---|---|---|
| Modern baseline | 26.2 | Existing source; build/test status must be verified |
| Modern adjacent versions | 26.1 and other releases up to 26.2 | Planned independent ports/builds |
| Recent legacy Fabric | 1.21.11, 1.21.4, 1.21.1 | Planned ports/builds |
| Older Fabric | 1.20.1, then earlier releases down to 1.14 | Planned ports/builds |
| Legacy minimum | 1.8.9 | Separate legacy-loader port required |
| Outside target range | Older than 1.8.9 or newer than 26.2 | Not supported by this target policy |

## Release checklist for each target

1. Select the exact Minecraft, Java, loader, API, mappings, and build-tool versions.
2. Compile the target-specific implementation.
3. Confirm the generated JAR declares the correct game and loader dependencies.
4. Launch that exact game version with its matching loader.
5. Test menu opening/closing, module toggles, keybinds, settings, HUD movement/scaling, and representative modules.
6. Publish a clearly named JAR, such as `glassmenu-1.0.0+mc1.21.1.jar`.
7. Advertise the version as supported only after all checks pass.

## Current status

This document defines the 1.8.9–26.2 compatibility goal; it does not claim that all versions in that range already work. The current source uses Minecraft 26.2-era APIs. Actual ports and builds are still required, and Minecraft 1.8.9 needs its own legacy-loader implementation.
