# Verification performed

Validated on 2026-10-13 in the provided Debian Linux x86-64 cloud workspace using the locally installed Java 21.0.12.1 JDK and Maven 3.9.9. All code began in an empty Git checkout.

## Build and test results

A clean Linux `verify` build completed successfully. A separate build with the Windows Maven profile completed successfully on this Linux machine. Both executed the same **52 tests**, with **0 failures, 0 errors, 0 skipped**:

| Suite | Tests | Behavior covered |
|---|---:|---|
| PhysicsTest | 11 | dimensions, eye height, gravity/drag profiles, jump apex, walking/sprint speeds, diagonal input, water drag/buoyancy, walls, sneak edges, fall damage and AABB clipping |
| WorldTest | 11 | starter layout, light emission/removal/occlusion, sky shadowing, renewable water, lava reactions, targeting faces/reach, sand, tree space/soil, save ID validation and biome colors |
| InventoryTest | 10 | overflow, tool metadata, recipes/mirroring/grid limits, ingredient preservation, furnace duration/full outputs/fuel buckets and durability |
| SaveTest | 4 | full state round trip, deterministic scheduled-fluid continuation, corrupt-save preservation and repeated atomic saves |
| GameTest | 12 | placement/overlap, container opening, buckets, hoes/crops, mining/tool wear/drops, harvesting rules, eating duration, pickup overflow, void death/respawn, creative placement and generated PCM |
| ClimateTest | 4 | biome IDs, temperature clamping/wetness, swamp override and furnace ignition/extinction light |

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

The latest runs reported:

```text
UI SMOKE PASS take/place/split/transfer/craft/close
SMOKE PASS frames=120 ticks=100 vertices=2124 health=20
UI SMOKE PASS take/place/split/transfer/craft/close
SMOKE PASS frames=35 ticks=35 vertices=2124 health=20
```

These checks execute native shader compilation, buffer uploads, generated textures, world rendering and UI rendering. They also use the actual GUI slot coordinates and click handlers to take logs, put one ingredient into the grid, craft planks, split a stack, Shift-transfer it, and verify conservation when the cursor/grid close. Each frame checks `glGetError`. The expected tick count excludes the deliberately paused recipe screen and is asserted. A nonempty generated world mesh and living player are required.

Screenshots:

- `target/smoke-game.png`: gameplay, biome-colored island/tree, HUD and first-person geometry.
- `target/smoke-inventory.png`: inventory/crafting screen over the rendered world.

Screenshots are captured from the actual framebuffer using Java ImageIO; no images were downloaded or imported. The earlier game screenshot was visually inspected during development. These are evidence of the implementation rendering, **not** comparisons with original Minecraft frames.

The two trace CSVs contain 160 rows of reproducible tick inputs/positions/velocities each, plus a header. They run without GLFW/display initialization. They are not original-game reference traces.

## Platform artifacts

The final platform-specific archives are in `target/distribution`:

| Archive | Verified contents | SHA-256 |
|---|---|---|
| `skyblock-classic-linux-0.1.0.jar` | 3 Linux native `.so` libraries, Java classes and manifest | `dd084bf9bdb45cf46e59154f6d83527b4969fc73a4314c37c332a12055e03390` |
| `skyblock-classic-windows-0.1.0.jar` | 3 Windows x64 native `.dll` libraries, Java classes and manifest | `ee28fb4401aaf5b1d76970c4fbb9a80d4b67c1f3aec052ad0a8d8d1d9e2a86bf` |

The build recreates the unshaded application JAR before shading on every package invocation, so repeated packaging does not recursively absorb a previously shaded JAR. The final default `target/skyblock-classic-0.1.0.jar` contains Linux natives.

**Platform limits:** Linux was executed on Debian/software Mesa, not Arch Linux or a hardware GPU. Arch installation commands are supplied; execution on Arch has not been tested. Windows was cross-packaged and its native resources inspected, but was **not executed on Windows**. ARM64, proprietary drivers and audio hardware were not exercised. The audio test verifies generated PCM; the graphical checks deliberately use `--no-audio`.

## Reuse and publication limits

Toolchains, Maven dependencies and source/build outputs are prepared in the current workspace. No game assets or credentials are needed. Future tasks can use the repository helper scripts. Xvfb and game processes must be started again; live processes are not snapshot state. Cloud install/start instructions are saved separately in the environment draft.

A saved draft is not publication and does not prove restoration in a new cloud task. GitHub Actions results are also not available until the project is pushed and the workflow actually runs. No original Minecraft 1.0 comparison was performed; the scope gaps in [FIDELITY.md](FIDELITY.md) remain.
