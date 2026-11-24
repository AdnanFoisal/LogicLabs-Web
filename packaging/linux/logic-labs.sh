#!/usr/bin/env bash
set -euo pipefail

# Resolve real script path even when invoked via /usr/bin/logic-labs symlink
SELF_PATH="$(readlink -f "$0")"
SELF_DIR="$(dirname "$SELF_PATH")"

if [[ -f "$SELF_DIR/app/LogicLabs-linux-all.jar" ]]; then
    APP_ROOT="$SELF_DIR"
elif [[ -f "$SELF_DIR/../app/LogicLabs-linux-all.jar" ]]; then
    APP_ROOT="$(cd "$SELF_DIR/.." && pwd)"
else
    MSG="Logic Labs application bundle (app/LogicLabs-linux-all.jar) was not found near $SELF_DIR."
    echo "ERROR: $MSG" >&2
    command -v zenity >/dev/null 2>&1 && zenity --error --title="Logic Labs" --text="$MSG" 2>/dev/null || true
    exit 2
fi

JAVA_BIN="$APP_ROOT/runtime/bin/java"
JAR_PATH="$APP_ROOT/app/LogicLabs-linux-all.jar"

if [[ ! -x "$JAVA_BIN" ]]; then
    MSG="Bundled Java runtime was not found at: $JAVA_BIN"
    echo "ERROR: $MSG" >&2
    command -v zenity >/dev/null 2>&1 && zenity --error --title="Logic Labs" --text="$MSG" 2>/dev/null || true
    exit 3
fi

# Low-RAM JVM tuning for 4 GB Linux Mint desktops (~180-240 MB RSS instead of ~500 MB G1GC)
JVM_MEM_FLAGS=(
    -Xms48m
    -Xmx320m
    -XX:+UseSerialGC
    -XX:MinHeapFreeRatio=15
    -XX:MaxHeapFreeRatio=30
    -XX:SoftRefLRUPolicyMSPerMB=50
    -Dfile.encoding=UTF-8
    -Dsun.java2d.dpiaware=true
    -Dawt.useSystemAAFontSettings=on
    -Dswing.aatext=true
)

FORCE_SOFTWARE=0
if [[ "${LOGICLABS_SOFTWARE_RENDER:-0}" == "1" ]]; then
    FORCE_SOFTWARE=1
fi
for arg in "$@"; do
    if [[ "$arg" == "--software" ]]; then
        FORCE_SOFTWARE=1
    fi
done

# Auto-detect CPU-emulated Mesa OpenGL (llvmpipe/softpipe/swrast): Skia's direct SOFTWARE
# rasterizer is dramatically faster and uses far less RAM than Mesa CPU OpenGL emulation.
if [[ "$FORCE_SOFTWARE" == "0" && -z "${LOGICLABS_RENDER_API:-}" ]]; then
    if command -v glxinfo >/dev/null 2>&1; then
        GL_RENDERER="$(timeout 1 glxinfo -B 2>/dev/null | grep -iE 'OpenGL renderer|Accelerated' || true)"
        if echo "$GL_RENDERER" | grep -qiE 'llvmpipe|softpipe|swrast|Accelerated:[[:space:]]*no'; then
            FORCE_SOFTWARE=1
        fi
    fi
fi

run_jvm() {
    local sw="$1"
    local render_args=()
    if [[ -n "${LOGICLABS_RENDER_API:-}" ]]; then
        render_args+=("-Dskiko.renderApi=${LOGICLABS_RENDER_API}")
    elif [[ "$sw" == "1" ]]; then
        render_args+=("-Dskiko.renderApi=SOFTWARE")
    fi

    "$JAVA_BIN" \
        "${JVM_MEM_FLAGS[@]}" \
        "${render_args[@]}" \
        "-Dlogiclabs.app.dir=$APP_ROOT" \
        -jar "$JAR_PATH"
}

if [[ "$FORCE_SOFTWARE" == "1" ]]; then
    exec "$JAVA_BIN" \
        "${JVM_MEM_FLAGS[@]}" \
        -Dskiko.renderApi=SOFTWARE \
        "-Dlogiclabs.app.dir=$APP_ROOT" \
        -jar "$JAR_PATH"
fi

START_TS="$(date +%s)"
set +e
run_jvm 0
EXIT_CODE=$?
set -e
END_TS="$(date +%s)"
ELAPSED=$((END_TS - START_TS))

# Automatic fallback to Skia software renderer if GPU initialization failed quickly on an older Linux Mint desktop
if [[ "$EXIT_CODE" -ne 0 && "$ELAPSED" -le 4 ]]; then
    echo "[Logic Labs] Hardware renderer exited rapidly (code $EXIT_CODE); retrying with Skia SOFTWARE renderer..." >&2
    exec "$JAVA_BIN" \
        "${JVM_MEM_FLAGS[@]}" \
        -Dskiko.renderApi=SOFTWARE \
        "-Dlogiclabs.app.dir=$APP_ROOT" \
        -jar "$JAR_PATH"
fi

exit "$EXIT_CODE"
