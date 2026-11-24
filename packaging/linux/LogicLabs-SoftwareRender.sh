#!/usr/bin/env bash
# Launches Logic Labs using Skia's software renderer.
# Use this if an older Linux Mint GPU driver causes OpenGL display glitches.
SELF_DIR="$(dirname "$(readlink -f "$0")")"
exec "$SELF_DIR/LogicLabs" --software "$@"
