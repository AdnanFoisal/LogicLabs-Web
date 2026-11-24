# Contributing to Logic Labs — Web & Multiplatform Edition

Thank you for helping improve **Logic Labs**! This repository is a **unified Kotlin Multiplatform project** where a single change in `commonMain` automatically updates the **Web PWA (WasmGC + JS fallback)**, **Linux Mint**, and **Windows 10/11** builds together.

---

## Development Setup

### Prerequisites
- **JDK 21** (Eclipse Temurin 21 recommended; JVM bytecode targets 17)
- **Git** (Gradle automatically provisions Node.js & Yarn for Web builds)

### Run Locally

```bash
# 1. Run Desktop JVM app locally
./gradlew :desktop-app:run

# 2. Run Web WasmGC dev server locally (opens in your browser with hot reload)
./gradlew :web-app:wasmJsBrowserDevelopmentRun

# 3. Build the complete dual-engine (WasmGC + JS) offline PWA bundle
./gradlew :web-app:assembleCombinedWebDist
# Output: web-app/build/dist/combined/
```

---

## Multiplatform Module Architecture

- **`:core-bridge`** (`commonMain`) — Pure Kotlin AD-200 breadboard topology, DSU netlist, 14 TTL 74xx IC models, and `IDEAL` vs `PRACTICAL` burnout physics.
- **`:core-data`** (`commonMain` + `jvmMain` + `webMain`) — `LogicLabsStorage` (`File` on Desktop, `localStorage` on Web), pure-Kotlin RFC 1950/1951 ZLIB `LOGIC-...` share codec, settings, session, and badge progress.
- **`:core-designsystem`** (`commonMain` + `jvmMain` + `webMain`) — 9 hardware color palettes, `LogicLabsFontStore` (Inter & JetBrains Mono), Skia `DesktopTextPaint`, `DesktopScrollbars`, and modal scaffolds.
- **`:feature-breadboard`** (`commonMain`) — 3-layer `BreadboardCanvas`, offscreen rasterized `BoardCache`, painters, and mouse + multi-touch pinch-to-zoom CAD gestures.
- **`:feature-instruments`** (`commonMain`) — K&H IDL-800A `TrainerConsoleDock`, `ClockDial`, switches, pulsers, LEDs, and 7-segment displays.
- **`:feature-tools`** (`commonMain` + `jvmMain` + `webMain`) — 49 coursework labs, Dual-Trace CRT Oscilloscope, Live Gate-Level Boolean Schematic, $2^N$ Verifier with pure-Kotlin HMAC-SHA256, `ConsoleSfx` (Java Sound / Web Audio API), and A4 PDF certificates (PDFBox / `PureKotlinPdfWriter`).
- **`:shared-app`** (`commonMain` + `jvmMain` + `webMain`) — Adaptive dual-mode Home Hub, Coursework, 74xx Reference, Settings, and Workbench UI (`≥ 840 dp` workstation & `< 840 dp` mobile drawer).
- **`:web-app`** (`wasmJs` + `js`) — Browser entry points, smart WasmGC/JS loader `index.html`, URL deep links (`?circuit=`, `?lab=`), offline Service Worker `sw.js`, PWA manifest, and Cloudflare `_headers`.
- **`:desktop-app`** (`jvm`) — Native Linux/Windows/macOS window host, menu bar, packaging tasks, and visual/performance audits.

---

## Verification Checklist Before Opening a PR

1. **Run all JVM unit, crypto, ZLIB interop, performance & visual tests:**
   ```bash
   ./gradlew :core-bridge:jvmTest :core-data:jvmTest :core-designsystem:jvmTest \
     :feature-breadboard:jvmTest :feature-tools:jvmTest :core-testing:test :desktop-app:test
   ```
2. **Verify BOTH Web engines (`wasmJs` + `js`) compile and package cleanly:**
   ```bash
   ./gradlew :web-app:assembleCombinedWebDist
   ```
3. **Keep `commonMain` 100% pure Kotlin** — no `java.*`, `javax.*`, or `Math.*` imports inside `commonMain`. Put platform-specific APIs behind `expect`/`actual` in `jvmMain` and `webMain`.
4. **License:** All contributions are licensed under **GPL-3.0-only** ([`LICENSE`](LICENSE)).
