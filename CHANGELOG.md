# Changelog

All notable changes to **Logic Labs — Web & Multiplatform Edition** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.1] — 2026-11-24

### Fixed — Narrow Mobile Browser Layout Polish (`< 600 dp`)
- **Coursework Cards & Multi-Output Truth Tables (`CourseworkTab`):**
  - Replaced the single-row unweighted badge layout in `TechnicalAccordionCard` with a responsive 2-row mobile header (`< 600 dp`) that gives experiment titles & subtitles full horizontal width on top and wraps individual `74xx` IC pills (`FlowRow`), `N BUILDS`, and `VERIFIED` badges cleanly alongside `OPEN` underneath — eliminating vertical single-character wrapping on multi-IC experiments (`2-Bit Magnitude Comparator`, `3-to-8 Line Decoder`, etc.).
  - Made wide multi-output preview truth tables (`TruthTableSection`) horizontally scrollable on compact screens and made the expanded launch button full-width on mobile.
- **74xx IC Reference & Pinout Inspector (`IcReferenceTab`, `IcPinoutInspectorSheet`, `DetectedLabsSection`):**
  - Added bounded `weight(1f)` flex sizing to IC part headers & gate mapping rows, switched `IcPinoutInspectorSheet`'s internal gate grid and pinout breakdown table to a spacious single-column layout on mobile (`< 600 dp`), and added `?v=1.3.1` PWA asset cache-busting for instant browser updates on refresh.

---

## [1.3.0] — 2026-11-23

### Fixed & Upgraded — Universal Multi-Device Zoom & Web Workstation Polish
- **Universal Pinch, Trackpad, Mouse Wheel & Keyboard Zoom (`ViewportZoomBridge` + `index.html`):**
  - Fixed **two-finger pinch-to-zoom on Android & iOS phones/tablets** by bridging multi-finger `TouchEvent.touches` centroids & distances directly into `BreadboardCanvas` and `BooleanDiagramView` at 60–120 Hz (bypassing Compose Web 1.7.0's single-finger `changedTouches` limitation).
  - Added smooth **laptop trackpad pinch (`Ctrl + Wheel`)** and **macOS/iPadOS Safari `GestureEvent` (`gesturechange`)** support while preventing browser page-zoom hijacking.
  - Added **`Ctrl + +` / `Ctrl + -` / `Ctrl + 0`** browser keyboard zoom shortcuts alongside `+` / `-` / `0` / `F`, plus **double-tap / double-click on empty breadboard** to toggle `100% Fit ↔ 220%` closeup at the tap point.
  - Upgraded the floating **Breadboard & Schematic Zoom HUD** (`−`, live resettable **`100%`** readout, `+`, `FIT`, `2.2×` loupe) with clear min/max limit states and live `ZOOM: %` telemetry in `ToolRail`.
- **Web Workstation Enhancements:**
  - **Edge-to-Edge Fullscreen Mode (`F11` & Toolbar Button):** Instant fullscreen toggle in the Workbench `CommandBar`, `DesktopHomeSidebar`, and `MobileHomeTopBar`.
  - **High-DPI Breadboard PNG Snapshot Export (`BreadboardSnapshotExporter` / `Ctrl+Shift+P`):** 1-click offscreen Skia PNG diagram download of the live wired breadboard from the Share & Board Actions dialogs.
  - **Drag-and-Drop `.logiclabs.json` Import:** Drop any `.logiclabs.json` project or `LOGIC-...` file anywhere onto the browser window to load it straight onto the bench.
  - **1-Click Offline PWA Install Prompt & Native Device Share Sheet:** Captures `beforeinstallprompt` for in-app PWA installation and integrates `navigator.share` on mobile/macOS browsers.
  - **Live Browser URL, Tab Title, Status-Bar Theme & Back-Button Sync:** Navigating labs/tabs updates the URL and mobile `<meta name="theme-color">`, and pressing Browser/Android Back returns safely to the Home Hub.
- **Mobile Workbench UX Upgrade:**
  - Added **vertical drag-to-resize** on the mobile IDL-800A console drawer handle (`< 840 dp`) and an **interactive collapsed Quick-I/O Strip** (`5V ON/OFF` + tappable `S7..S0` switches paired with live `L7..L0` LEDs) so phone users can keep 80%+ screen height for wiring while toggling switches live.
  - Made `ControlsGuideDialog` (`F1` / `?`) single-column responsive on narrow mobile screens.

---

## [1.2.0] — 2026-11-23

### Added — Web Edition & Unified Kotlin Multiplatform Architecture
- **100% Feature-Parity Browser Workstation (`:web-app`):** Runs the entire K&H IDL-800A Digital Logic Trainer & AD-200 Breadboard directly inside any modern browser with zero backend servers and zero installation.
- **Dual-Engine Smart Loader (WasmGC + Automatic JS Fallback):** Probes WebAssembly GC support in `< 1 ms` inside `index.html`, loading fast **Kotlin/Wasm (`wasmJs` + Skiko)** on Chrome 119+, Firefox 120+, Edge 119+, and Safari 18.2+, while transparently falling back to **Kotlin/JS IR (`js` + Skiko)** on older iOS devices and Chromebooks.
- **Adaptive Dual-Mode Multi-Device UI (`:shared-app`):**
  - **PC / Laptop / Landscape Tablet (`≥ 840 dp`):** Full side-by-side breadboard + resizable IDL-800A Trainer Console dock, mouse CAD (cursor-anchored scroll zoom, middle/Space drag pan, right-click menus, hover voltages), and complete keyboard hotkeys (`1–8`, `Q/E`, `P`, `K/.`, `W/V/S`, `C/B/O/D/F1`).
  - **Phones & Portrait Tablets (`< 840 dp`):** Compact hardware top header & bottom navigation bar (`Projects`, `Coursework`, `74xx ICs`, `Settings`), full-width pinch-to-zoom breadboard, and a collapsible bottom Trainer Console drawer with an always-visible live `L7..L0` mini LED status strip.
- **Offline Installable PWA:** Bundled `manifest.webmanifest`, versioned `sw.js` Service Worker caching bundled fonts and active engine assets, and Cloudflare Pages `_headers` — works completely offline after first load.
- **Cross-Platform Web Adapters (Zero Backend):**
  - **Web Audio API (`ConsoleSfx`):** Exact procedural click/pop/tick hardware waveforms synthesized in-browser.
  - **Pure-Kotlin RFC 1950/1951 ZLIB (`PureKotlinZlib`):** Binary-compatible `LOGIC-...` circuit share codes across Android, Linux/Windows Desktop, and Web.
  - **Shareable Direct Links (`?circuit=LOGIC-...` & `?lab=7`):** 1-click **"COPY DIRECT LINK"** in the Share dialog; opening a shared URL loads the circuit live onto the breadboard.
  - **Pure-Kotlin RFC 2104 HMAC-SHA256 & Client-Side A4 PDF 1.4 Writer (`PureKotlinPdfWriter`):** Exhaustive $2^N$ verification seals match Android & Desktop bit-for-bit and download printable A4 `.pdf` lab certificates straight in the browser.
  - **Browser `localStorage` Persistence + `.logiclabs.json` Import/Export:** Autosave drafts, saved projects, settings, themes, and lab progress persist locally in the browser with JSON file download/upload.
- **Unified 11-Module KMP Repository:** Migrated `:core-bridge`, `:core-data`, `:core-designsystem`, `:feature-breadboard`, `:feature-instruments`, `:feature-tools`, and `:shared-app` to Kotlin Multiplatform (`jvm` + `wasmJs` + `js`) while preserving 100% of Desktop JVM tests and Linux/Windows packaging.
- **Automated Free GitHub Pages Deployment:** Added `.github/workflows/deploy-web.yml` to test all modules and auto-deploy the dual-engine offline PWA to GitHub Pages on every push to `main`.

---

## [1.1.3] — 2026-11-23

### Performance & Desktop Polish (Carried Over Across All Platforms)
- **11.2× Faster Rasterized Breadboard (`BoardCache`):** RAM-capped offscreen Skia `Image` blitting (`3.52 ms/frame`, `8.70 MB` cap).
- **Zero-Stall Async Desktop Console Audio (`ConsoleSfx`):** Dedicated daemon audio queue (`0.0117 ms/click`).
- **Hardware-Themed Scrollbars Everywhere (`DesktopScrollbars`):** Vertical & horizontal scrollbars, horizontal wheel scrolling, `PageUp`/`PageDown`, `ClockDial` `Shift+Wheel` + `−`/`+`, 2-column 74xx datasheets & Settings rack, Quick-Start Classic Labs shelf, and curriculum unit filters.
