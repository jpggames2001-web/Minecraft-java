# Verification performed

Validated on 2026-10-13 in the provided Debian Linux x86-64 cloud workspace using the locally installed Java 21.0.12.1 JDK and Maven 3.9.9. All code began in an empty Git checkout.

## Build and test results

A clean Linux `verify` build completed successfully. A separate build with the Windows Maven profile completed successfully on this Linux machine. Both executed the same **100 tests**, with **0 failures, 0 errors, 0 skipped**:

| Suite | Tests | Behavior covered |
|---|---:|---|
| PhysicsTest | 11 | dimensions, eye height, gravity/drag profiles, jump apex, walking/sprint speeds, diagonal input, water drag/buoyancy, walls, sneak edges, fall damage and AABB clipping |
| WorldTest | 11 | starter layout, light emission/removal/occlusion, sky shadowing, renewable water, lava reactions, targeting faces/reach, sand, tree space/soil, save ID validation and biome colors |
| InventoryTest | 10 | overflow, tool metadata, recipes/mirroring/grid limits, ingredient preservation, furnace duration/full outputs/fuel buckets and durability |
| SaveTest | 6 | state round trip, deterministic scheduled-fluid continuation, corrupt-save preservation, atomic saves, legacy-yaw migration and future-version rejection |
| GameTest | 12 | placement/overlap, container opening, buckets, hoes/crops, mining/tool wear/drops, harvesting rules, eating duration, pickup overflow, void death/respawn, creative placement and generated PCM |
| ClimateTest | 4 | biome IDs, temperature clamping/wetness, swamp override and furnace ignition/extinction light |
| MouseLookTest | 12 | real camera turn direction, independent inversion, sensitivity/clamping, capture transitions, runtime setting changes, movement/sprint consistency and block targeting |
| MouseSettingsTest | 6 | defaults, session persistence, invalid values, failed-write behavior, reset/atomic saves and sensitivity bounds |
| MenuTest | 19 | screen hierarchy, disabled buttons, world/server CRUD, forms/keyboard submission, video, volumes, bindings/conflicts, language and packs |
| ClientPersistenceTest | 9 | world folders/seeds/spawn safety, server persistence/address parsing, real TCP ping response, local texture directories/ZIPs, invalid packs, preserved user files and binding names |

Commands actually exercised:

```bash
bash scripts/cloud-setup.sh
bash scripts/maven.sh -B -Pwindows verify
bash scripts/maven.sh -B clean verify
bash scripts/smoke.sh
bash scripts/run.sh --trace target/physics-specified.csv
bash scripts/run.sh --physics release-reference --trace target/physics-reference.csv
```

All Bash helper scripts passed `bash -n`. Publishing helper scripts were syntax-checked but **not executed** during initial validation. GitHub publication is a separate operation; consult Git history for the current published commit. PowerShell helpers and the GitHub Actions workflow have not executed here.

## Real renderer and interface checks

The GLFW/OpenGL executable ran under Xvfb on **Mesa llvmpipe 25.0.7 / OpenGL 4.5 core**, satisfying the requested 3.3 context. The smoke helper allocates a fresh display and cleans up only its own Xvfb process. It resets the display-number output before each invocation, avoiding a stale-display startup race discovered during repeatability testing.

The standard renderer runs reported:

```text
UI SMOKE PASS take/place/split/transfer/craft/close
CONTROLS SMOKE PASS nested-menu/inversion/sensitivity/rebinding/persistence/reset
SMOKE PASS frames=120 ticks=100 vertices=2124 health=20
UI SMOKE PASS take/place/split/transfer/craft/close
SMOKE PASS frames=35 ticks=35 vertices=2124 health=20
```

These checks execute native shader compilation, buffer uploads, generated textures, world rendering and UI rendering. They also use the actual GUI slot coordinates and click handlers to take logs, put one ingredient into the grid, craft planks, split a stack, Shift-transfer it, and verify conservation when the cursor/grid close. The controls smoke check opens Esc → Options → Controls, toggles both axes, changes sensitivity, reloads the properties file, rebinds Forward, returns through parent menus with Esc, and resets defaults. Smoke preferences are isolated in a fresh temporary directory and never write the user’s actual controls file. Each frame checks `glGetError`. The expected tick count excludes the deliberately paused controls screen and is asserted. A nonempty generated world mesh and living player are required.

Screenshots:

- `target/smoke-game.png`: gameplay, biome-colored island/tree, HUD and first-person geometry.
- `target/smoke-inventory.png`: inventory/crafting screen over the rendered world.
- `target/smoke-controls.png`: Controls screen with both axes inverted and sensitivity changed with the slider and Forward rebound.

Screenshots are captured from the actual framebuffer using Java ImageIO; no images were downloaded or imported. The gameplay and Controls screenshots were visually inspected during development. These are evidence of the implementation rendering, **not** comparisons with original Minecraft frames.

The two trace CSVs contain 160 rows of reproducible tick inputs/positions/velocities each, plus a header. They run without GLFW/display initialization. They are not original-game reference traces.

The expanded native run also reported:

```text
MENUS SMOKE PASS worlds/servers/video/anaglyph/audio/language/packs/death
SMOKE PASS frames=120 ticks=100 vertices=2124 health=20
```

It created and loaded a real world, saved it back to title, renamed and confirmed deletion, edited a persisted server, rendered connection/direct-connect screens, switched video options including anaglyph, selected a language, exported/loaded a native texture template, and returned from a dead player to title before respawning. Fifteen screenshots live at `target/menu-*.png`; title, world selection and video captures were visually inspected. This run deliberately uses isolated temporary data and does not modify real user worlds/options/packs. Multiplayer login is not validated or implemented.

## Platform artifacts

The final platform-specific archives are in `target/distribution`:

| Archive | Verified contents | SHA-256 |
|---|---|---|
| `skyblock-classic-linux-0.1.0.jar` | 3 Linux native `.so` libraries, Java classes and manifest | `790025b9b64dbeb5b2ad3188b07648cddaba39fe2268a77077831ae7baa92899` |
| `skyblock-classic-windows-0.1.0.jar` | 3 Windows x64 native `.dll` libraries, Java classes and manifest | `94df69df75c1b563340a75df4a95b8b54952856fff183ae7f9a17e40a6537956` |

The build recreates the unshaded application JAR before shading on every package invocation, so repeated packaging does not recursively absorb a previously shaded JAR. The final default `target/skyblock-classic-0.1.0.jar` contains Linux natives.

**Platform limits:** Linux was executed on Debian/software Mesa, not Arch Linux or a hardware GPU. Arch installation commands are supplied; execution on Arch has not been tested. Windows was cross-packaged and its native resources inspected, but was **not executed on Windows**. ARM64, proprietary drivers and audio hardware were not exercised. The audio test verifies generated PCM; the graphical checks deliberately use `--no-audio`.

## Reuse and publication limits

Toolchains, Maven dependencies and source/build outputs are prepared in the current workspace. No game assets or credentials are needed. Future tasks can use the repository helper scripts. Xvfb and game processes must be started again; live processes are not snapshot state. Cloud install/start instructions are saved separately in the environment draft.

A saved draft is not publication and does not prove restoration in a new cloud task. GitHub Actions results are also not available until the project is pushed and the workflow actually runs. No original Minecraft 1.0 comparison was performed; the scope gaps in [FIDELITY.md](FIDELITY.md) remain.
