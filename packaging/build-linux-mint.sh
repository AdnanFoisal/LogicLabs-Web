#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VERSION="1.1.3"
OUT_DIR="${LOGICLABS_LINUX_OUT_DIR:-$ROOT_DIR/dist/linux}"
PORTABLE_NAME="LogicLabs-Linux-Mint-Portable"
PORTABLE_DIR="$OUT_DIR/$PORTABLE_NAME"
DEB_STAGE="$ROOT_DIR/dist-cache/deb-stage"
DEB_FILE="$OUT_DIR/logic-labs_${VERSION}_amd64.deb"
CACHE_DIR="$ROOT_DIR/dist-cache"
JRE_TGZ="$CACHE_DIR/temurin21-jre-linux-x64.tar.gz"

echo "[1/6] Building Linux x64 fat JAR..."
./gradlew :desktop-app:linuxFatJar --no-daemon

JAR_FILE="$ROOT_DIR/desktop-app/build/libs/LogicLabs-linux-all.jar"
if [[ ! -f "$JAR_FILE" ]]; then
    echo "ERROR: Expected fat JAR not found at $JAR_FILE" >&2
    exit 1
fi

echo "[2/6] Ensuring Eclipse Temurin 21 Linux x64 JRE (GLIBC 2.15+ compatible)..."
mkdir -p "$CACHE_DIR" "$OUT_DIR"
if [[ ! -f "$JRE_TGZ" ]]; then
    curl -fSL -o "$JRE_TGZ" \
        "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse"
fi

TMP_JRE="$CACHE_DIR/jre-linux-extract"
rm -rf "$TMP_JRE"
mkdir -p "$TMP_JRE"
tar -xzf "$JRE_TGZ" -C "$TMP_JRE"
TOP_JRE_DIR="$(find "$TMP_JRE" -mindepth 1 -maxdepth 1 -type d | head -n 1)"

if [[ ! -x "$TOP_JRE_DIR/bin/java" ]]; then
    echo "ERROR: Bundled runtime/bin/java missing after JRE extraction!" >&2
    exit 1
fi

echo "[3/6] Assembling Portable Linux Mint Bundle ($PORTABLE_DIR)..."
rm -rf "$PORTABLE_DIR" "$OUT_DIR/$PORTABLE_NAME.tar.gz"
mkdir -p "$PORTABLE_DIR/app" "$PORTABLE_DIR/runtime"

cp packaging/linux/logic-labs.sh "$PORTABLE_DIR/LogicLabs"
cp packaging/linux/LogicLabs-SoftwareRender.sh "$PORTABLE_DIR/LogicLabs-SoftwareRender.sh"
cp packaging/linux/install-desktop-shortcut.sh "$PORTABLE_DIR/install-desktop-shortcut.sh"
cp packaging/linux/README-LINUX-MINT.txt "$PORTABLE_DIR/README-LINUX-MINT.txt"
cp packaging/linux/README-LINUX-MINT.txt "$OUT_DIR/README-LINUX-MINT.txt"
cp desktop-app/src/main/resources/icon.png "$PORTABLE_DIR/icon.png"
cp LICENSE "$PORTABLE_DIR/LICENSE"
cp THIRD_PARTY_NOTICES.md "$PORTABLE_DIR/THIRD_PARTY_NOTICES.md"
cp "$JAR_FILE" "$PORTABLE_DIR/app/LogicLabs-linux-all.jar"
cp -a "$TOP_JRE_DIR/." "$PORTABLE_DIR/runtime/"

chmod 755 "$PORTABLE_DIR/LogicLabs" \
    "$PORTABLE_DIR/LogicLabs-SoftwareRender.sh" \
    "$PORTABLE_DIR/install-desktop-shortcut.sh"

echo "[4/6] Archiving Portable Tarball ($OUT_DIR/$PORTABLE_NAME.tar.gz)..."
tar -C "$OUT_DIR" -czf "$OUT_DIR/$PORTABLE_NAME.tar.gz" "$PORTABLE_NAME"

echo "[5/6] Staging Debian/Ubuntu/Linux Mint .deb package..."
rm -rf "$DEB_STAGE" "$DEB_FILE"
mkdir -p \
    "$DEB_STAGE/DEBIAN" \
    "$DEB_STAGE/opt/logic-labs/app" \
    "$DEB_STAGE/opt/logic-labs/bin" \
    "$DEB_STAGE/opt/logic-labs/runtime" \
    "$DEB_STAGE/usr/bin" \
    "$DEB_STAGE/usr/share/applications" \
    "$DEB_STAGE/usr/share/doc/logic-labs" \
    "$DEB_STAGE/usr/share/icons/hicolor/48x48/apps" \
    "$DEB_STAGE/usr/share/icons/hicolor/128x128/apps" \
    "$DEB_STAGE/usr/share/icons/hicolor/256x256/apps" \
    "$DEB_STAGE/usr/share/icons/hicolor/512x512/apps"

cp packaging/linux/logic-labs.sh "$DEB_STAGE/opt/logic-labs/bin/logic-labs"
chmod 755 "$DEB_STAGE/opt/logic-labs/bin/logic-labs"
ln -s /opt/logic-labs/bin/logic-labs "$DEB_STAGE/usr/bin/logic-labs"

cp "$JAR_FILE" "$DEB_STAGE/opt/logic-labs/app/LogicLabs-linux-all.jar"
cp -a "$TOP_JRE_DIR/." "$DEB_STAGE/opt/logic-labs/runtime/"
cp packaging/linux/logic-labs.desktop "$DEB_STAGE/usr/share/applications/logic-labs.desktop"
cp LICENSE "$DEB_STAGE/usr/share/doc/logic-labs/copyright"
cp THIRD_PARTY_NOTICES.md "$DEB_STAGE/usr/share/doc/logic-labs/THIRD_PARTY_NOTICES.md"
cp packaging/linux/README-LINUX-MINT.txt "$DEB_STAGE/usr/share/doc/logic-labs/README-LINUX-MINT.txt"

# Generate crisp multi-resolution hicolor PNG icons for Cinnamon / MATE / Xfce menus & taskbar
python3 - <<PYEOF
from PIL import Image
src = Image.open("$ROOT_DIR/desktop-app/src/main/resources/icon.png").convert("RGBA")
for sz in (48, 128, 256, 512):
    out = "$DEB_STAGE/usr/share/icons/hicolor/%dx%d/apps/logic-labs.png" % (sz, sz)
    src.resize((sz, sz), Image.LANCZOS).save(out, format="PNG")
PYEOF

INSTALLED_KB="$(du -sk "$DEB_STAGE/opt" "$DEB_STAGE/usr" | awk '{s+=$1} END{print s}')"

cat > "$DEB_STAGE/DEBIAN/control" <<EOF
Package: logic-labs
Version: ${VERSION}
Section: education
Priority: optional
Architecture: amd64
Installed-Size: ${INSTALLED_KB}
Depends: libc6 (>= 2.17), libx11-6, libxext6, libxrender1, libxtst6, libxi6, libfontconfig1, libasound2 | libasound2t64
Recommends: xdg-utils, zenity
Maintainer: Logic Labs Desktop Port <logiclabs@localhost>
Homepage: https://github.com/AdnanFoisal/LogicLabs
Description: Digital Electronics Lab & K&H IDL-800A Breadboard Workstation
 Offline digital twin of the K&H IDL-800A Digital Logic Trainer and AD-200
 breadboard with 14 TTL 74xx ICs, 49 guided coursework experiments, dual-channel
 CRT oscilloscope, live Boolean schematic, 2^N truth-table verifier with A4 PDF
 certificates, synthesized console audio, and full mouse & keyboard controls.
 Bundled with Eclipse Temurin 21 JRE (no separate Java installation required).
EOF

cat > "$DEB_STAGE/DEBIAN/postinst" <<'EOF'
#!/bin/sh
set -e
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database -q /usr/share/applications || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -q -f -t /usr/share/icons/hicolor || true
fi
exit 0
EOF

cat > "$DEB_STAGE/DEBIAN/postrm" <<'EOF'
#!/bin/sh
set -e
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database -q /usr/share/applications || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -q -f -t /usr/share/icons/hicolor || true
fi
exit 0
EOF

chmod 755 "$DEB_STAGE/DEBIAN/postinst" "$DEB_STAGE/DEBIAN/postrm"

echo "[6/6] Building .deb package ($DEB_FILE)..."
dpkg-deb --root-owner-group --build "$DEB_STAGE" "$DEB_FILE"

rm -rf "$TMP_JRE" "$DEB_STAGE"

echo "DONE:"
ls -lh "$DEB_FILE" "$OUT_DIR/$PORTABLE_NAME.tar.gz" "$PORTABLE_DIR/LogicLabs"
