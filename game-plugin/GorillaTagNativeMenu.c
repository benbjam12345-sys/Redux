# REDUX Unity In-Game Menu Template

This folder contains source code, not a prebuilt Quest mod. It is separate from the REDUX Android ADB companion APK.

## What this template implements

- Toggles a world-space Unity menu on the rising edge of LEFT trigger + LEFT grip held together.
- Provides two presentation modes: a panel near the left controller (hand mode) and a floating computer panel.
- Lets the user switch menu profiles (REDUX, LUDEX/LUNEX, STRIPES ADB, DELTA, MOONLIT V2) and categories (Movement, Visuals, Player, Settings).
- Uses the right controller trigger to select buttons by pointing at them.
- Clearly labels gameplay options as previews. The option labels are not wired to movement, visuals, or other game logic.

## Compatibility requirements

This is a generic Unity-side starting point. It must be compiled against the exact Unity version and managed assemblies used by the target Quest game, and loaded by a compatible mod loader. The MelonLoaderEntry.cs file is an example entry point for a compatible MelonLoader/LemonLoader environment; loader/API versions can differ.

1. Obtain permission to mod the target game and confirm which mod loader/version it uses.
2. Build the two C# files as a managed plugin for that game's Unity/loader versions, referencing the matching UnityEngine, UnityEngine.UI, UnityEngine.XRModule, and MelonLoader assemblies.
3. Install it only through the supported loader for that game.
4. Test on a private test session first. If the game crashes, remove the plugin through the loader's normal disable/uninstall procedure.

The Android REDUX APK cannot, on its own, intercept controller input from another running game or draw over that game's Unity scene. The native game integration and loader compatibility are essential. No other menu's private binaries are copied or activated by profile selection.