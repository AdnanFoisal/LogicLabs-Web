#!/usr/bin/env bash
set -euo pipefail

SELF_DIR="$(dirname "$(readlink -f "$0")")"
APP_DIR="$HOME/.local/share/applications"
mkdir -p "$APP_DIR"

DESKTOP_FILE="$APP_DIR/logic-labs-portable.desktop"
cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=Logic Labs (Portable)
GenericName=Digital Electronics Bench
Comment=K&H IDL-800A Digital Logic Trainer & AD-200 Breadboard Workstation
Exec="$SELF_DIR/LogicLabs"
Icon=$SELF_DIR/icon.png
Terminal=false
Categories=Education;Electronics;Science;Development;
Keywords=logic;digital;breadboard;74xx;ttl;circuit;simulator;idl-800a;
StartupNotify=true
StartupWMClass=com-logiclabs-app-MainKt
Actions=software-render;

[Desktop Action software-render]
Name=Launch in Software Render Mode
Exec="$SELF_DIR/LogicLabs" --software
EOF

chmod +x "$DESKTOP_FILE"
command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$APP_DIR" 2>/dev/null || true

echo "Installed Linux Mint Application Menu shortcut at:"
echo "  $DESKTOP_FILE"
echo "You can now launch 'Logic Labs (Portable)' from your Linux Mint Menu!"
