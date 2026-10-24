# SimpleSkinBackport

HEAVY WIP

Attempts to backport the modern skin format with mixins in a *simple, compatible* way. I created this to hopefully solve the issue of there being no simple skin backport once and for all. I always tell people Ears was the best one, but in reality it cannot maintain perfect compatibility due to adding extra features that some may not want.

This mod focuses on backporting the skin format and maintaining compatibility with other mods, with optional extrusion of the outer skin layers. For additional player customization, try [MorePlayerModels](https://www.curseforge.com/minecraft/mc-mods/more-player-models) or [Ears](https://modrinth.com/mod/ears/versions).

Alternate skin server mods should just work, but would likely need manual patching for slim arm support.

Current Features:
- Optional per-pixel thickness for outer skin layers, including first-person sleeves and player heads
- 1.8 skin format
  - Slim arm support
  - Extended hat layer support
- Translucent hat layer support
- Fix overlap in hat layer when wearing a head
- Hat layers on head blocks
- New default skins alongside just Steve
- Patch for Twilight Forest giants
- Patch for Botania's Gaia Guardian
- Works with OptiFine
- Works with Smart Moving animations
  - Untested with upstream Smart Moving, please use the builds made by makamys: [Smart Render](https://github.com/makamys/SmartRender) [Smart Moving](https://github.com/makamys/SmartMoving)

## Extruded skin layers

The effect is disabled by default. In `config/simpleskinbackport/main.cfg`, set `B:enabled=true` under `client` → `extrudedlayers`.

Extrusion works with standard 64×64 classic and slim skins. The category also contains settings for each outer part, thickness and render distance. The defaults add 0.2 model pixels of thickness and use flat layers beyond 12 blocks. Reduce the thickness if a modded armor model fits closely to the skin. These settings are client-side.

Todo:
- 3D head rendering in inventory + mods todo:
  - Twilight Forest
  - Iquana Tinker Tweaks
  - Tinkers' Construct gold upgrades
  - HEE Enderman
  - Gaia Guardian
- Toggling individual hat layers for each limb
- Config
  - Fix regular heads not working without a UUID when worn as a helmet or viewed in the inventory
  - Toggle head block inventory icon render
