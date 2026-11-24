# Changelog

All notable changes to **Logic Labs — Web & Multiplatform Edition** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
