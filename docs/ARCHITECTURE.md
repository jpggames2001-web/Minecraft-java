# Architecture

The simulation is independent of OpenGL. Tests instantiate worlds and players without GLFW or a display. `Main` owns native startup, input, a fixed 50 ms tick accumulator, desktop screens and shutdown.

```text
src/main/java/dev/skyblock/
  Main.java                  CLI, GLFW window/input, fixed ticks, launch/save lifecycle
  world/
    World.java               finite storage, biomes, updates, fluids, collision, raycast, light
    Block.java               stable project IDs, hardness, emission, geometry/atlas properties
    Aabb.java, RayHit.java    collision clipping and block targeting values
    Biome.java               release-era identities and temperature/rainfall values
    ClimateColorMap.java     generated palettes and bilinear sampling
  game/
    Player.java              movement, collisions, survival and inventory ownership
    PhysicsProfile.java      supplied versus reference constants
    PhysicsTrace.java        display-free deterministic CSV trace
    Controls.java            immutable per-tick input
    MouseLook.java           captured cursor deltas, recapture anchors, independent axis inversion
    MouseSettings.java       validated inversion/sensitivity configuration
    Game.java                targeting, breaking/placing, use, buckets, farming and drops
    Item.java, Stack.java    item metadata, limits and immutable counted/damaged stacks
    Inventory.java           transfer with explicit overflow
    Recipes.java             shaped/mirrored/shapeless matching, smelting and fuel tables
    Container.java           chest/furnace storage and tick state
    Drop.java                pickup delay, item physics and despawn
  render/
    Assets.java              generated RGBA atlas and item sprites
    Shader.java              checked compilation/linking and uniforms
    Mesh.java                vertex buffers and native memory ownership
    WorldMesher.java         exposed faces, plants, AO, simple generated entity/item geometry
    Renderer.java            chunks, camera, fog, sky, translucent pass and first-person hand
  ui/
    BitmapFont.java          original ASCII pixel glyph arrays
    Canvas.java              batched GL pixel quads and item icons
    Interface.java           screen transitions, storage/crafting clicks, HUD and guide
  audio/SynthAudio.java       generated PCM and bounded asynchronous sound queue
  persistence/SaveStore.java  bounded/validated GZIP serialization and atomic replacement
  persistence/MouseSettingsStore.java  atomic, user-wide mouse preferences
src/test/java/dev/skyblock/   physics, world, climate, recipes, interaction and save tests
scripts/                     platform launch, cloud tool bootstrap, graphics checks, publishing
```

## Tick and rendering lifecycle

GLFW input is sampled into `Controls`. At 20 Hz, `Game` updates player physics, world block ticks, furnace state and items, then computes the ray target and applies held actions. World updates use a priority queue with deduplicated due times. A deterministic xorshift random state drives block ticks and tree generation; the state and pending ticks are persisted.

Lighting rebuilds on block edits and furnace ignition/extinction. Meshes rebuild when world revision changes. Each chunk has separate opaque and transparent VBOs. Transparent chunks are ordered by distance; individual faces are not sorted. The complete world is small, but heavy fluid edits still trigger full light/mesh rebuilds. This is a correctness-first finite-world implementation, not a high-performance infinite-terrain engine.

Camera positions interpolate between fixed ticks; mouse angles update at render cadence. Positive yaw turns right: yaw 0 faces -Z and yaw 90 faces +X. Forward movement, strafing, sprint jumps, camera rendering and block targeting share that convention. MouseLook resets its cursor anchor on capture transitions, applies persisted independent X/Y inversion and sensitivity, and clamps pitch. Desktop resize updates projection and UI scaling. GLFW callbacks, GL programs, native float buffers, VAOs/VBOs, atlas textures and the audio line are closed on shutdown. The audio queue is bounded and a missing sound device falls back to silence.

## Persistence and ownership

`Stack` is immutable. `Inventory.add` returns overflow so callers retain or drop it. Crafting consumes one item per occupied recipe cell only when the result can fit on the cursor or in inventory. UI closes return cursor and grid items before save, avoiding inaccessible items in saved worlds. Autosave occurs only in gameplay; quitting from a container returns carried items and saves container storage.

A save is written to a sibling temporary file and moved over the selected target, with atomic replacement where supported. Load validates IDs, dimensions, inventory limits, counts, coordinates, clocks and container locations. GZIP validates its checksum. A load failure aborts before playing; an existing corrupt save is preserved. The format version is now 2; version-1 saves negate the legacy yaw during load so their world-space view stays unchanged. Subsequent version-2 loads preserve yaw directly. This private format is not Mojang-compatible. Mouse preferences live separately from world saves in a properties file and save on each successful Controls-screen change. Failed writes leave the active settings unchanged and are shown in the menu. Graphical smoke tests isolate preferences in a temporary directory and clean it up.

## Asset boundary

There are no game image/audio/font/model resources in the repository. The atlas and PCM are generated at runtime. Models are numeric geometry and glyphs are original arrays. Build dependencies, JDK/Maven binaries and graphics infrastructure are external software dependencies, not imported game assets. There are no runtime network requests or account credentials.
