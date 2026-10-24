# Minecraft version compatibility

GlassMenu is being prepared for multiple Minecraft Java versions using separate builds.
A JAR compiled against one Minecraft version must not be assumed to work on another.

## Compatibility policy

- Keep the existing Minecraft 26.2 implementation as the modern baseline.
- Maintain version-specific implementations where Minecraft GUI, HUD, keybinding, rendering, or Fabric APIs differ.
- Publish a separate JAR for every supported Minecraft/Fabric target.
- Never mark a version as supported until its build succeeds and the mod has been smoke-tested in that game version.
- Keep shared module logic and settings portable where possible; isolate version-specific Minecraft API calls behind adapters.

## Planned rollout

| Tier | Target versions | Status |
|---|---|---|
| Modern baseline | 26.2 | Existing implementation; build/test status must be verified |
| Modern adjacent versions | 26.1 and later compatible releases | Planned port and independent build |
| Recent legacy | 1.21.11, 1.21.4, 1.21.1 | Planned ports and independent builds |
| Older releases | 1.20.1, then earlier Fabric-supported releases | Expand after the recent legacy port is stable |

The long-term goal is to reach as far back as practical (potentially Minecraft 1.14+), but older releases will be listed as supported only after their dedicated implementation and build are available.

## Why separate builds?

Minecraft changes rendering and GUI APIs, keybinding APIs, mappings, Java requirements, Fabric API versions, and Loom configuration over time. In particular, Minecraft 26.1+ and 1.21.11-and-older use different Loom plugin IDs/tooling paths. A single source tree with one dependency set cannot safely cover this entire range.

## Release checklist for each target

1. Select the exact Minecraft, Java, Fabric Loader, Fabric API, mappings, and Loom versions.
2. Compile the target-specific source set.
3. Confirm the generated JAR declares the correct Minecraft dependency and Java requirement.
4. Launch that exact Minecraft version with Fabric and the matching Fabric API.
5. Test opening/closing the menu, module toggles, keybinds, settings persistence, HUD movement/scaling, and representative modules.
6. Publish a clearly named JAR, for example `glassmenu-1.0.0+mc1.21.1.jar`.
7. Add the version to the supported list only after all checks pass.

## Current status

This document describes the rollout plan, not a claim that all listed versions already work. The current source uses Minecraft 26.2-era APIs and needs real ports before older versions can be advertised as compatible.
