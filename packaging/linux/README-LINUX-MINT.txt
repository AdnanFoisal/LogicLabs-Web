======================================================================
  LOGIC LABS v1.1.3 — LINUX MINT EDITION (Ubuntu / Debian x64)
  K&H IDL-800A Digital Logic Trainer & AD-200 Breadboard Workstation
======================================================================

TWO WAYS TO RUN ON LINUX MINT (19 / 20 / 21 / 22 & LMDE — NO JAVA NEEDED)
-------------------------------------------------------------------------

Option A — Official .deb Installer (Recommended on Linux Mint):
  1. Double-click "logic-labs_1.1.3_amd64.deb" in Nemo/Caja/Thunar
     (opens Linux Mint's GDebi Package Installer -> click "Install Package"),
     OR run in a terminal:
         sudo apt install ./logic-labs_1.1.3_amd64.deb
  2. Launch "Logic Labs" from the Linux Mint Application Menu
     (Education / Programming), or run `logic-labs` from terminal.
  3. To uninstall later: `sudo apt remove logic-labs`

Option B — Portable Archive (Zero root/sudo required):
  1. Extract LogicLabs-Linux-Mint-Portable.tar.gz anywhere:
         tar -xzf LogicLabs-Linux-Mint-Portable.tar.gz
  2. Double-click "LogicLabs" (or run `./LogicLabs` in terminal).
  3. Optional: run `./install-desktop-shortcut.sh` to add Logic Labs to
     your Linux Mint Application Menu without sudo.

OLDER LINUX MINT LAPTOPS / GPU COMPATIBILITY
--------------------------------------------
Logic Labs bundles Eclipse Temurin 21 (GLIBC 2.15+) and Skiko Linux x64
(GLIBC 2.17+), so it runs on Linux Mint 19, 20, 21, 22, and LMDE.
If an older GPU driver rejects OpenGL, Logic Labs automatically falls
back to Skia software rendering. You can also launch explicitly in
software mode with:
  - Installed .deb : Right-click Logic Labs in Menu -> "Launch in Software Render Mode"
                     or run `logic-labs --software`
  - Portable       : Run `./LogicLabs-SoftwareRender.sh`

WHERE YOUR PROJECTS & SETTINGS ARE SAVED
----------------------------------------
- Default: ~/.local/share/LogicLabs/
  (Open quickly from Settings -> "Open Projects & Data Folder", or
   File -> "Open Projects Folder" in the top menu bar.)
- Portable / USB mode:
  Create an empty file named "portable.txt" right beside the LogicLabs
  launcher (`touch portable.txt`). All projects, settings, and progress
  will be stored inside a local `data/` subfolder.

QUICK CONTROLS CHEAT SHEET (PRESS F1 IN-APP FOR THE FULL GUIDE)
---------------------------------------------------------------
Mouse Pointer:
  - Scroll Wheel             : Zoom smoothly toward mouse cursor
  - Middle-Drag / Space+Drag : Pan breadboard in any mode
  - Left-Drag (empty board)  : Pan breadboard in Roam / Select mode
  - Left-Click / Drag (Wire) : Click hole A then B, or click-drag to wire
  - Right-Click              : Context menu on wire/IC/board (or cancel wire)
  - Drag IC / Wire Endpoint  : Move IC across columns/trenches, re-wire pin
  - Double-Click IC          : Open live IC Pinout & Truth Table Inspector
  - Hold Alt (or press L)    : 2.2x optical magnifying loupe

Keyboard Shortcuts:
  - 1 .. 8                   : Toggle Data Switches SW0 .. SW7
  - Q / E                    : Hold Momentary Pulser A / Pulser B
  - P                        : Toggle Master +5V Power Supply
  - K / .                    : Start/Pause Clock  /  Single-Step Clock
  - W / V / S                : Wire Draw / Roam / Wire Select mode
  - R / M / Del              : Rotate IC 180 deg / Manhattan Wire / Delete
  - Arrow Keys               : Nudge selected IC (or pan board)
  - Ctrl+Z / Ctrl+Y          : Multi-step Undo / Redo
  - F or 0 / + / -           : Fit view / Zoom in / Zoom out
  - C / B / O / D            : Chip Catalog / 49 Labs / Oscilloscope / Schematic
  - F5                       : Run exhaustive 2^N Truth-Table Verification
  - F1 or ?                  : Open Controls & Shortcuts Guide
  - Ctrl+S / Ctrl+E / Ctrl+O : Save / Export JSON / Import JSON
  - Ctrl+1..4                : Switch Home tabs (Projects/Labs/ICs/Settings)

CREDITS & LICENSE
-----------------
Original Logic Labs Android application by Adnan Foisal (CUET CSE):
  https://github.com/AdnanFoisal/LogicLabs
Simulation kernel extracted from hneemann/Digital (GPL-3.0).
Licensed under GPL-3.0-only. See LICENSE and THIRD_PARTY_NOTICES.md.
