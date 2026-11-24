#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BUNDLE_NAME="LogicLabs-Windows-Portable"
DIST_DIR="$ROOT_DIR/dist"
BUNDLE_DIR="$DIST_DIR/$BUNDLE_NAME"
CACHE_DIR="$ROOT_DIR/dist-cache"
JRE_ZIP="$CACHE_DIR/temurin21-jre-win-x64.zip"

echo "[1/5] Building Windows x64 fat JAR..."
./gradlew :desktop-app:windowsFatJar --no-daemon

JAR_FILE="$ROOT_DIR/desktop-app/build/libs/LogicLabs-windows-all.jar"
if [[ ! -f "$JAR_FILE" ]]; then
    echo "ERROR: Expected fat JAR not found at $JAR_FILE" >&2
    exit 1
fi

echo "[2/5] Cross-compiling native Win64 GUI LogicLabs.exe..."
x86_64-w64-mingw32-windres packaging/windows/launcher.rc -O coff -o packaging/windows/launcher.res
x86_64-w64-mingw32-gcc -O2 -Wall -Wextra -mwindows -municode -s -static-libgcc \
    packaging/windows/launcher.c packaging/windows/launcher.res \
    -o packaging/windows/LogicLabs.exe
rm -f packaging/windows/launcher.res

echo "[3/5] Ensuring Eclipse Temurin 21 Windows x64 JRE..."
mkdir -p "$CACHE_DIR"
if [[ ! -f "$JRE_ZIP" ]]; then
    curl -fSL -o "$JRE_ZIP" \
        "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"
fi

echo "[4/5] Assembling $BUNDLE_DIR..."
rm -rf "$BUNDLE_DIR" "$DIST_DIR/$BUNDLE_NAME.zip"
mkdir -p "$BUNDLE_DIR/app" "$BUNDLE_DIR/runtime"

cp packaging/windows/LogicLabs.exe "$BUNDLE_DIR/LogicLabs.exe"
cp packaging/windows/LogicLabs-SoftwareRender.bat "$BUNDLE_DIR/LogicLabs-SoftwareRender.bat"
cp packaging/windows/README-WINDOWS.txt "$BUNDLE_DIR/README-WINDOWS.txt"
cp LICENSE "$BUNDLE_DIR/LICENSE"
cp THIRD_PARTY_NOTICES.md "$BUNDLE_DIR/THIRD_PARTY_NOTICES.md"
cp "$JAR_FILE" "$BUNDLE_DIR/app/LogicLabs-windows-all.jar"

# Unpack Temurin JRE stripping its top-level versioned folder into runtime/
TMP_JRE="$CACHE_DIR/jre-extract"
rm -rf "$TMP_JRE"
mkdir -p "$TMP_JRE"
unzip -q "$JRE_ZIP" -d "$TMP_JRE"
TOP_JRE_DIR="$(find "$TMP_JRE" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
cp -a "$TOP_JRE_DIR/." "$BUNDLE_DIR/runtime/"
rm -rf "$TMP_JRE"

if [[ ! -f "$BUNDLE_DIR/runtime/bin/javaw.exe" ]]; then
    echo "ERROR: runtime/bin/javaw.exe missing after JRE extraction!" >&2
    exit 1
fi

echo "[5/5] Archiving $DIST_DIR/$BUNDLE_NAME.zip..."
(cd "$DIST_DIR" && zip -qr "$BUNDLE_NAME.zip" "$BUNDLE_NAME")

echo "DONE:"
ls -lh "$BUNDLE_DIR/LogicLabs.exe" "$BUNDLE_DIR/app/LogicLabs-windows-all.jar" "$DIST_DIR/$BUNDLE_NAME.zip"
