# Fidelity status and reference contract

The requested target is Minecraft Java Edition **1.0**, with a fixed Skyblock world. This implementation does **not** satisfy 100% fidelity, and cannot honestly be described as an identical release or production-ready replacement for that game. The following distinctions are part of the delivered implementation rather than claims of completed parity.

## Evidence and constants

The user's numerical specification is the direct acceptance input. No original game assets, original game binaries or decompiled game source were downloaded or imported, and no differential execution against version 1.0 was performed. Tests verify specific implemented behavior; they do not establish original-game equivalence.

The latest request contains a physics inconsistency: it calls `0.98` a horizontal air multiplier and `0.91` an ice multiplier. Common release-era descriptions use `0.91` for horizontal airborne velocity, `0.98` for vertical drag, `0.98` as ice slipperiness, and a further `0.91` horizontal damping factor on the ground. Two explicit profiles avoid silently treating these as the same model:

| Constant or rule | Default `specified` | `release-reference` |
|---|---|---|
| Tick rate | 20 Hz | 20 Hz |
| Width / height / eye height | 0.6 / 1.8 / 1.62 | Same |
| Gravity / vertical drag | `(vy - 0.08) * 0.98` | Same |
| Jump initial velocity | 0.42 | Same |
| Horizontal air drag | 0.98 | 0.91 |
| Standard-block slipperiness | 0.6 | 0.6 |
| Ice slipperiness | 0.91 | 0.98 |
| Ground damping | slipperiness × 0.91 | Same |
| Ground acceleration | `0.1 * 0.16277136 / damping^3` | Same |
| Forward/strafe scaling | × 0.98, normalized if length > 1 | Same |
| Sprint ground acceleration | × 1.3 | Same |
| Air acceleration | 0.02 (0.026 sprint) | Same |
| Water | acceleration 0.02, drag 0.8, vertical gravity 0.02 | Same |
| Lava | acceleration 0.02, drag 0.5, vertical gravity 0.02 | Same |
| Small-velocity cutoff | absolute velocity < 0.005 → 0 | Same |
| Sneak input | × 0.3 with edge clipping | Same |
| Step height | 0.5 | Same |

On standard blocks, constant forward input converges to about **0.21586 blocks/tick before damping**, or **4.317 blocks/second**. Sprinting converges to about 0.28062 blocks/tick. With the small-velocity cutoff applied, the measured jump apex is **1.2491870944 blocks**. Integrating gravity without that cutoff would produce 1.252203352512; the cutoff is material to the result. The doubles used here do not reproduce every rounding effect of the original float calculations.

`--trace PATH` writes a headless 160-tick CSV containing inputs, positions, velocities and grounded state for a fixed walk/jump/sprint/sneak sequence. This is comparison evidence for this implementation, not an original-game trace. `release-reference` is a named hypothesis about the historical constants, not a certified emulation mode.

## Lighting and climate

Skylight enters each unobstructed column at 15. Direct vertical sky rays do not attenuate through air; lateral propagation drops by one. Leaves and fluid blocks add attenuation. Block light decreases by at least one per step, with opacity affecting propagation. Opaque blocks stop light. Torches emit 14, lava 15, and a burning furnace 13. Removing lights and editing geometry rebuilds both light fields.

This is more precise than treating all skylight as a uniform -1 flood: doing that would make the bottom of unobstructed columns dark, unlike the target. The block-light fields are tested; renderer brightness uses a simplified exponential curve, not the original lightmap calculation.

The biome registry contains 16 release-era identities and climate parameters. Forest, Desert and Taiga columns occur in the fixed map. Temperature is clamped to [0,1] and multiplied by rainfall for climate lookup. Independently generated 256×256 maps are sampled with bilinear interpolation. The sky hue/saturation formula uses the classic temperature relationship.

**The original grass/foliage color maps are intentionally not imported.** Generated palette values are approximate and cannot be pixel-identical to those maps. Swamp has a fixed color override. Weather, snowfall, biome-specific spawning, and most biome vegetation behavior are not implemented.

## Implemented core and known differences

| Area | Implemented | Remaining differences |
|---|---|---|
| World | Finite Skyblock, height 128, column biomes, oak tree, starter chest | Custom island/outpost layout; no procedural overworld, Nether or End |
| Movement | Fixed ticks, AABBs, jump, sprint, sneak edges, steps, ice profiles, water/lava drag | Exact float rounding, fluid currents and liquid ledge escape, ladder/vine/cobweb movement, flying and all movement quirks are not covered |
| Survival | Health, hunger/saturation/exhaustion, food delay, fall/void/lava/cactus damage, drowning, death and respawn | Armor, status effects, sleeping, XP, combat entities, fire spread/burning and full damage rules are absent |
| Inventory | 36 slots, hotbar, splitting, merging, transfer, cursor stack conservation, dropped items | Full original creative inventory, armor slots, all item metadata and all container automation are absent |
| Crafting | Implemented recipe subset including wooden/stone tools, iron pickaxe, table, chest, furnace, bucket, bread, torches, bone meal | Most 1.0 items/recipes, diamond/gold tool progression, brewing and enchanting are absent |
| Blocks | Registry of the implemented materials, hardness/tool drops, placement, source buckets, farming | Full block roster, redstone, doors, pistons, rails, boats, beds, slabs/stairs, orientation, original partial-block targeting/geometry are absent |
| Fluids | Scheduled flow, bounded drop search, infinite water sources, water/lava solidification | Not the complete historical fluid algorithm; fluid surfaces/current forces and update ordering are not certified |
| Block updates | Grass changes, saplings/oak growth, leaves decay, farmland hydration, wheat, cactus support, sand falling | Sand settles instantly rather than as a falling entity; decaying leaves/unsupported plants do not produce original drops; cactus height growth, weather freezing, all update rules are absent |
| Rendering | Modern GL 3.3 shaders, VBOs, generated atlas, fog, basic AO, celestial sprites, first-person hand/item, transparent chunks | Original 1.0 used a different rendering pipeline; texture UV orientation, transparent-face sorting, celestial orientation, clouds, particles, damage cracks, animation and complete lightmap/fog behavior differ |
| Interface | Original pixel font/UI, inventory, chest, furnace, craft, title, pause, death and guide screens | Original font glyphs, GUI coordinates/art, localization, chat, options and interface behavior are not identical |
| Audio | Original synthesized steps, dig/place, damage, pickup/click buffers; graceful silent fallback | No original audio or music; synthesis does not reproduce original recordings; sounds serialize on one playback line |
| Persistence | Project save format, atomic replacement, validation, deterministic random/block tick continuity | No original world format or network/protocol compatibility; no multiplayer |

No placeholder methods are presented as implemented systems. Missing release-wide features are actual scope gaps. Shader-based rendering and original procedural assets satisfy the modern-library/no-import constraints, but those choices do not establish a pixel-identical original rendering pipeline.

## What would establish fidelity

Certification requires an independently lawful 1.0 reference runtime, deterministic input/world fixtures, matched update order and arithmetic, and comparisons of position/velocity traces, inventories, block update sequences, light levels, interface events and rendered frames. None of those comparisons ran in this session. A passing local test is not a substitute for this differential evidence.

Useful technical documentation for the modern runtime: [LWJGL guide](https://www.lwjgl.org/guide), [GLFW documentation](https://www.glfw.org/docs/latest/), [JOML](https://github.com/JOML-CI/JOML), and [Maven](https://maven.apache.org/guides/). These describe the libraries; they are not sources proving version 1.0 gameplay parity.
