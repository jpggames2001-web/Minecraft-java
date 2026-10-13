# Skyblock Classic

A Java 21 desktop voxel game built in this repository from an empty checkout. It has a fixed Skyblock island, original procedural 16×16 artwork, an original bitmap font and synthetic PCM sound. It uses Maven, LWJGL 3, GLFW, OpenGL 3.3 and JOML.

**This is not a verified, complete recreation of Minecraft Java Edition 1.0.** The core Skyblock workflow is implemented and tested, but release-wide systems and exact visual/behavioral parity remain incomplete. Read [the fidelity report](docs/FIDELITY.md) before treating it as a reference implementation. No original Minecraft assets, source code, game binaries or external artwork were imported.

## Ready-to-run downloads

Install Java 21, then download the JAR matching your operating system:

- [Windows x64 JAR](https://github.com/jpggames2001-web/Minecraft-java/raw/refs/heads/main/downloads/skyblock-classic-windows-0.1.0.jar)
- [Linux x64 JAR](https://github.com/jpggames2001-web/Minecraft-java/raw/refs/heads/main/downloads/skyblock-classic-linux-0.1.0.jar)
- [Checksums](downloads/SHA256SUMS) and [Portuguese launch instructions](downloads/README.md).

These archives are included in this repository so you can test without Maven. Run `java -jar skyblock-classic-windows-0.1.0.jar` on Windows, or the Linux filename on Linux. A graphical desktop and OpenGL 3.3 driver are required. Windows execution is still unverified; the Linux build passed the documented graphical checks.

## Build and run

Requires a **Java 21 JDK**, Maven 3.9+, and a graphics driver supporting OpenGL 3.3. Java 17 is not supported by this build. Third-party programming libraries and build tools are downloaded by Maven; game assets are generated in memory.

### Arch Linux (x86-64)

```bash
sudo pacman -S --needed jdk21-openjdk maven libx11 libxcursor libxrandr libxinerama libxi mesa
# JAVA_HOME must select Java 21 if another Java installation is the default.
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
mvn -B clean verify
bash scripts/run.sh
```

A working X11 or Wayland desktop is required. GLFW chooses a supported backend. Proprietary GPU drivers may provide OpenGL instead of Mesa. The Linux ARM64 Maven profile activates on `aarch64`, but that platform has not been executed here.

### Windows (x86-64, PowerShell)

Install a Java 21 JDK and Maven, add their `bin` directories to `PATH`, then:

```powershell
java -version
javac -version
mvn -version
mvn -B clean verify
powershell -ExecutionPolicy Bypass -File scripts/run.ps1
```

The Windows Maven profile selects `natives-windows` automatically. Build separately on each platform; a Linux JAR includes Linux natives. Windows builds and simulation tests are configured in GitHub Actions but **have not run on an actual Windows machine in this session**.

### This cloud workspace

```bash
cd /workspace/Minecraft-java
bash scripts/cloud-setup.sh
bash scripts/maven.sh -B verify
bash scripts/smoke.sh
```

The Debian cloud bootstrap installs tools locally under `/workspace/.minecraft-tools`, verifies Debian package signatures/metadata through APT, and verifies Maven's SHA-512 checksum. It adapts Maven to the injected HTTPS proxy without recording credentials. No system administrator privileges are needed.

The cloud machine has a virtual display for automated graphics checks. Onboarding does not provide an interactive game stream or localhost web preview. To play, run the desktop build on your own graphical Linux/Windows machine.

## Controls

| Action | Control |
|---|---|
| Move / look | WASD / mouse |
| Jump / swim up | Space |
| Sneak, guard the island edge | Shift |
| Sprint | Ctrl or double-tap W |
| Break / place or use | Hold left mouse / right mouse |
| Select hotbar | 1–9 or mouse wheel |
| Inventory | E |
| Drop one held item | Q |
| Recipe and controls guide | R |
| Debug / screenshot | F3 / F2 |
| Pause | Esc |
| Inventory move / split / transfer | Left click / right click / Shift-click |
| Eat | Hold right mouse with food selected |

Open **Esc → Options → Controls** (or Options → Controls on the title screen) to view the current keybindings, toggle **Invert Mouse X** and **Invert Mouse Y**, and adjust sensitivity with its slider. Both axes default to OFF. Moving the mouse right turns right, and moving it down looks down; each inversion changes only its own axis. Reset restores non-inverted controls and the original sensitivity (100% = 0.15 degrees per cursor pixel).

Changes are saved immediately to `~/.skyblock-classic/controls.properties`, independently of world saves. To use another file, launch with `--settings local/controls.properties`. If a setting cannot be saved, the controls screen displays the error and retains the previous saved setting. Click a binding to assign a keyboard key or mouse button. Conflicts appear in red; Clear unbinds the action and Reset restores defaults. Scroll to access every binding.

Existing version-1 worlds load with their camera orientation preserved. New saves use version 2 to record the corrected yaw convention; older binaries cannot load version-2 saves.

On the title screen choose **Singleplayer → Create New World → SkyBlock**. Existing legacy saves appear in Select World, and named worlds support load/rename/confirmed delete. Video, sound, language and native texture-pack options are available under Options. Multiplayer currently performs real legacy server pings but cannot join networked worlds. See [MENUS.md](docs/MENUS.md) for implemented flows and precise limitations.

Start by opening the chest, collecting the starter supplies, harvesting the tree, and crafting planks, sticks, a table and a wooden pickaxe. Ice above solid ground becomes water when broken. Put water next to **flowing** lava to produce cobblestone; water beside a lava source produces obsidian. Keep a sapling and avoid losing essential supplies into the void.

## What runs

- A bounded 64×128×64 world, 16 chunks, an L-shaped dirt/grass island, oak tree, starter chest, small desert and snowy outposts, and void beyond it.
- A fixed 20 Hz player simulation: AABB collision, walking, sprinting, jumping, sneaking at edges, water/lava resistance, fall/void damage, hunger, eating, drowning and respawn.
- A block registry, block breaking with hardness/tool requirements, placement, buckets, tool wear, pickup/dropped items, chest storage and player inventory.
- Shaped/mirrored and shapeless crafting, 2×2 and 3×3 grids, fuel-based furnaces with 200-tick smelting, hoes, wheat, hydration, bone meal and sapling growth.
- 15-level skylight and block-light flooding, torch/lava/burning-furnace emission, day/night sky changes, 16 biome identifiers with climate parameters and generated interpolated tint palettes.
- Chunk VBO meshes, texture atlas, transparency, basic ambient occlusion, fog, generated celestial billboards, first-person hand/item geometry and an original pixel UI.
- A component/screen-stack menu system with world/server management, remapped controls, persisted options, synthetic volume channels, language selection and native custom texture packs.
- GZIP saves with corruption checks, atomic replacement, persistent inventories, entities, furnace progress, random state and scheduled fluid/block ticks. A corrupt save is rejected before gameplay and is not silently overwritten.

The legacy save defaults to `~/.skyblock-classic/world.dat` (Windows: under your user profile). New named worlds live under `.skyblock-classic/worlds/<UUID>`. Screenshots are written alongside the save. The format is private to this project and does not read Minecraft worlds.

```bash
bash scripts/run.sh --save local/world.dat
bash scripts/run.sh --fresh --save local/new-world.dat
bash scripts/run.sh --creative --save local/creative.dat
bash scripts/run.sh --physics release-reference
bash scripts/run.sh --no-audio
bash scripts/run.sh --trace target/physics.csv
```

`--fresh` prepares a new legacy island and **replaces the selected legacy save on startup**; use a separate path when keeping an existing world. Creative mode currently provides unlimited placement, immunity and a starter palette; it does not implement the complete original creative inventory or flying system.

## Verification and architecture

`mvn verify` runs meaningful simulation, crafting, inventory, climate, integrated interaction, persistence, and synthesized audio tests. `scripts/smoke.sh` builds and starts the real native renderer under Xvfb, checks OpenGL errors and generated geometry, and captures game/inventory screenshots. Results and their limits are in [VERIFICATION.md](docs/VERIFICATION.md). [ARCHITECTURE.md](docs/ARCHITECTURE.md) describes the modules.

## Publish to GitHub

The checked-out Git repository already exists. Complete terminal commands and Bash/PowerShell initialization scripts are documented in [DEPLOYMENT.md](docs/DEPLOYMENT.md). The scripts verify the build before committing and pushing to `main`, and do not force-push.

This project's source and original assets are MIT licensed. Third-party libraries retain their own licenses. Minecraft is a Mojang/Microsoft trademark; this project is unaffiliated.
