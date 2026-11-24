======================================================================
  LOGIC LABS v1.1.3 — WINDOWS DESKTOP EDITION (Windows 10 / 11 x64)
  K&H IDL-800A Digital Logic Trainer & AD-200 Breadboard Workstation
======================================================================

HOW TO RUN
----------
1. Extract the ENTIRE ZIP archive into a folder (for example: C:\LogicLabs).
   Do NOT run LogicLabs.exe directly from inside the Windows ZIP preview.
2. Double-click LogicLabs.exe.
   - No Java installation is required (Eclipse Temurin 21 JRE is bundled).
   - Works offline with zero accounts, zero telemetry, and no admin prompt.

OLDER WINDOWS 10 LAPTOPS / GPU COMPATIBILITY
--------------------------------------------
LogicLabs.exe automatically falls back to software rendering if an older
Windows 10 GPU driver fails to initialize Direct3D. You can also launch
with software rendering explicitly at any time by running:
    LogicLabs-SoftwareRender.bat

WHERE YOUR PROJECTS & SETTINGS ARE SAVED
----------------------------------------
- Default: %APPDATA%\LogicLabs\
  (Open quickly from Settings -> "Open Projects & Data Folder", or
   File -> "Open Projects Folder" in the top menu bar.)
- Portable / USB mode:
  Create an empty file named "portable.txt" right beside LogicLabs.exe.
  Logic Labs will automatically store all projects, settings, and progress
  in a local "data\" subfolder instead of %APPDATA%.

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
