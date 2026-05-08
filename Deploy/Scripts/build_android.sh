#!/usr/bin/env bash
# EverTask Android Build Script (Bash)
# Usage: ./build_android.sh

set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KMP_DIR="$REPO_ROOT/kmp"
OUTPUT_DIR="$REPO_ROOT/Deploy/Android/Build/Output"

echo "========================================"
echo " EverTask Android Build Script"
echo "========================================"

GRADLEW="$KMP_DIR/gradlew"
if [[ ! -f "$GRADLEW" ]]; then
    echo "ERROR: gradlew not found at $GRADLEW"
    exit 1
fi

cd "$KMP_DIR"

echo ""
echo "[1/4] Running unit tests..."
"$GRADLEW" :androidApp:testDebugUnitTest --no-daemon
"$GRADLEW" :shared:testDebugUnitTest --no-daemon

echo ""
echo "[2/4] Building release AAB..."
"$GRADLEW" :androidApp:bundleRelease --no-daemon

echo ""
echo "[3/4] Copying outputs to Deploy folder..."
mkdir -p "$OUTPUT_DIR/mapping"

cp "$KMP_DIR/androidApp/build/outputs/bundle/release/androidApp-release.aab" "$OUTPUT_DIR/evertask-1.0.0-release.aab"
echo "  Copied release AAB"

if [[ -f "$KMP_DIR/androidApp/build/outputs/apk/debug/androidApp-debug.apk" ]]; then
    cp "$KMP_DIR/androidApp/build/outputs/apk/debug/androidApp-debug.apk" "$OUTPUT_DIR/evertask-1.0.0-debug.apk"
    echo "  Copied debug APK"
fi

if [[ -d "$KMP_DIR/androidApp/build/outputs/mapping/release" ]]; then
    cp "$KMP_DIR/androidApp/build/outputs/mapping/release/"* "$OUTPUT_DIR/mapping/"
    echo "  Copied ProGuard mapping files"
fi

echo ""
echo "[4/4] Running deploy validation..."
bash "$REPO_ROOT/Deploy/Scripts/validate_deploy.sh"

echo ""
echo "========================================"
echo " Build completed successfully!"
echo "========================================"
