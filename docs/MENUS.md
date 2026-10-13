# Menu implementation and fidelity status

This build adds a functional component/screen-stack menu system. **It is not a 100% faithful recreation of every Minecraft Java 1.0 screen or behavior.** The local workflows below are implemented and tested. Multiplayer world login and original texture-pack compatibility remain missing backend capabilities, and the generated visual assets do not establish pixel parity.

## Architecture

`ui/menu/ScreenStack` owns parent/child screens and closes resources when screens leave. Each menu is a separate `MenuScreen` class that lays out its own widgets and handles render/input/lifecycle. Shared widgets in `ui/widget` implement buttons, nine-slice procedural backgrounds, sliders, text fields and scrollable selection lists. Buttons handle hover, focus and disabled state; Tab/Enter, text editing, clipboard operations and pointer dragging are supported. Text fields retain state across frames and resize. ESC pops a child screen; pause resumes gameplay; death and title do not accidentally resume a dead world.

`InventoryScreen`, `CraftingScreen`, `ChestScreen` and `FurnaceScreen` have separate lifecycles and share the existing inventory-slot component. Their screens do not pause the simulation; cursor/crafting-grid items return safely when closing. Dead players' cursor/grid items are dropped instead of being restored to their inventory. The core block, inventory and furnace operations remain tested.

`MenuContext.Actions` binds screen actions to the live application. The main loop consumes remapped bindings, forwards GLFW character/key events to the active screen, releases mouse capture for GUI input and applies client settings. Loading a world invalidates the renderer's cached world identity, avoiding reuse of meshes from another world with the same revision number.

## Implemented workflows

| Screen | Functional behavior | Fidelity limits |
|---|---|---|
| Main menu | Singleplayer, Multiplayer, Mods/Texture Packs, Options, Quit; generated dirt background and bitmap title | Original panorama/logo/splash assets and exact positioning/metrics are not reproduced |
| Select World | List, select/load, rename, confirmed delete, create; names and seeds persist | Private project world format; no original Minecraft worlds or conversion/version screens |
| Create World | Name, numeric/text/random seed, Default/SkyBlock toggle, validation, keyboard submission | Default is finite custom terrain, not Minecraft 1.0's infinite generator; requested SkyBlock toggle is custom |
| Multiplayer | Persistent add/edit/delete/list; validated direct-connect/Join address; connection progress/cancel/status | No vanilla protocol-22 login, authentication, world/chunk/entity synchronization or multiplayer gameplay |
| Connecting | Real pre-1.4 `0xFE` server-list ping, bounded UTF-16 response, MOTD/player count, timeout/error/cancellation | A successful ping explicitly reports that this build cannot enter multiplayer worlds; Join does not start a networked game |
| Options | Nested video, controls, sound, language and pack screens; correct parent return | Requested option grouping includes additions from later releases; not the complete historical options UI |
| Video | Fancy/Fast leaf opacity, render distance/fog, smooth AO toggle, two-eye red/cyan anaglyph, GUI scaling, FPS limit and VSync | Advanced OpenGL uses frustum culling rather than original occlusion queries; fog/lightmaps/rendering remain simplified |
| Controls | Scrollable full project binding list, keyboard/mouse assignment, red conflict marking, clear/reset, X/Y inversion and sensitivity | Covers implemented project controls, not all historical GUI details; Chat/Player List have no multiplayer backend |
| Music/Sounds | Persisted master/music/effect sliders; synthesis playback gain updates; generated background phrases | No original music or sounds; channel menu is a requested modern extension, not original 1.0 grouping |
| Language | Persisted English/Portuguese/Spanish selection, translated common menu labels | Partial catalogs, ASCII translations, generated glyphs; not original localization coverage |
| Texture Packs | Directory/ZIP discovery, preview, apply/default, refresh, template export and folder action | Native `atlas.png` 128x128 format, not original `terrain.png`/GUI/font texture-pack layout; no mods loader or remote downloads |
| Pause | Resume, nested Options, Save and Quit to Title | Original full statistics/achievements menus and transitions are absent |
| Death | Respawn and Title; dead saves do not force title back to death | Original score and complete damage/death GUI behavior are absent |
| Inventory/crafting/chest/furnace | Existing working slots, transfer/splitting, craft output, chest storage and timed fuel/smelting | Armor slots, original artwork and every original item/container interaction remain incomplete |

The requested menu list mixes original-era features with later additions. In particular, a separate Language screen, resource-pack terminology and independent master/music/effect controls do not describe a single original 1.0 menu tree. They are provided where useful as requested project features, without claiming historical equivalence.

## Data and persistence

All client data normally lives beside `~/.skyblock-classic/controls.properties`:

- `controls.properties`: existing independent mouse inversion/sensitivity.
- `options.properties`: video, volume, language, selected texture pack and bindings.
- `servers.properties`: the user's saved server list.
- `worlds/<UUID>/world.dat` plus `world.properties`: named worlds and metadata.
- `texturepacks/`: user-authored native packs and exported procedural templates.

The legacy file chosen by `--save` appears in Select World if it exists. New worlds use unique folders under `worlds`. Existing world files are never renamed based on the entered display name. Delete asks for confirmation and preserves unrelated user files in a world folder. A load failure reports an error instead of treating a corrupt file as a fresh world. Atomic properties writes apply settings only after persistence succeeds.

## Native texture-pack format

Choose **Texture Packs → Export Template** to create a new `generated-template-N` directory. Edit its **128x128 `atlas.png`** and optional first line of `pack.txt`; refresh, select and Apply. A ZIP with those files at its root is also accepted. The preview shows the atlas; runtime switching uploads it to the existing GL texture. UI button tiles remain generated. The template is this project's original artwork and contains no original game assets.

Images are dimension/size checked before loading; ZIP contents are read without extraction. Invalid packs remain unselectable. No packs, fonts, sounds, artwork or models are downloaded. Importing user-authored local packs is the explicitly requested custom-pack capability; no original Mojang pack was imported during development.

## Verification

Model tests cover screen transitions, disabled actions, world/server CRUD, validation, focused keyboard input, binding conflicts/clears/reset, sliders, localization, local pack parsing, persistence, actual legacy TCP ping packets and unrelated-file preservation. The graphical smoke additionally runs real world creation/loading/rename/delete, native screen rendering, options changes (including anaglyph), template export/application and death/title/respawn.

Screenshots are generated at `target/menu-*.png`. These demonstrate this build's output, not comparisons against original 1.0. Windows-native cross-packaging is separate from execution on Windows. Full original-game fidelity and multiplayer compatibility have not been validated.
