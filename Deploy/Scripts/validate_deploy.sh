#!/usr/bin/env bash
# EverTask Deploy Validation Script (Bash)
# Usage: ./validate_deploy.sh

set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="$REPO_ROOT/Deploy"

REQUIRED_FILES=(
    "Android/Build/Output/evertask-1.0.0-release.aab"
    "Android/Build/Output/evertask-1.0.0-debug.apk"
    "Android/Build/Output/mapping/mapping.txt"
    "Android/StoreListing/title.txt"
    "Android/StoreListing/short_description.txt"
    "Android/StoreListing/full_description.txt"
    "Android/StoreListing/Icon/icon_512x512.png"
    "Android/StoreListing/FeatureGraphic/feature_graphic_1024x500.png"
    "Android/Policy/data_safety_form.md"
    "Android/Policy/data_safety_labels.json"
    "Android/Policy/content_rating_guide.md"
    "Android/Build/build_instructions.md"
    "Android/Build/keystore_setup.md"
    "Review/ReviewNotes.txt"
    "Review/DemoVideoScript.txt"
    "Review/ContactInfo.txt"
    "Review/TestAccounts.txt"
    "Review/SubmissionChecklist.txt"
    "Review/ReviewResponseTemplates.txt"
    "Shared/Legal/privacy_policy.md"
    "Shared/Legal/terms_of_service.md"
    "Shared/Legal/data_deletion_policy.md"
    "Shared/Legal/eula.md"
    "Marketing/press_release.md"
    "README.md"
    "CHECKLIST.md"
    "INDEX.md"
)

ISSUES=()

echo "========================================"
echo " EverTask Deploy Validation"
echo "========================================"

for rel_path in "${REQUIRED_FILES[@]}"; do
    full_path="$DEPLOY_DIR/$rel_path"
    if [[ -f "$full_path" ]]; then
        size=$(stat -f%z "$full_path" 2>/dev/null || stat -c%s "$full_path" 2>/dev/null)
        echo "[OK] $rel_path ($size bytes)"
    else
        echo "[MISSING] $rel_path"
        ISSUES+=("$rel_path")
    fi
done

# Size checks
AAB_PATH="$DEPLOY_DIR/Android/Build/Output/evertask-1.0.0-release.aab"
if [[ -f "$AAB_PATH" ]]; then
    aab_size=$(stat -f%z "$AAB_PATH" 2>/dev/null || stat -c%s "$AAB_PATH" 2>/dev/null)
    if [[ $aab_size -lt 1048576 ]]; then
        echo "[WARN] AAB is suspiciously small ($aab_size bytes)"
        ISSUES+=("AAB size check")
    else
        aab_mb=$(echo "scale=2; $aab_size / 1048576" | bc)
        echo "[OK] AAB size: ${aab_mb} MB"
    fi
fi

APK_PATH="$DEPLOY_DIR/Android/Build/Output/evertask-1.0.0-debug.apk"
if [[ -f "$APK_PATH" ]]; then
    apk_size=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null)
    if [[ $apk_size -lt 1048576 ]]; then
        echo "[WARN] APK is suspiciously small ($apk_size bytes)"
        ISSUES+=("APK size check")
    else
        apk_mb=$(echo "scale=2; $apk_size / 1048576" | bc)
        echo "[OK] APK size: ${apk_mb} MB"
    fi
fi

# Image dimension checks (using Python/PIL)
python3 - <<'PY' 2>/dev/null || python - <<'PY' 2>/dev/null || echo "[INFO] Image dimension checks skipped (Python/PIL unavailable)"
from PIL import Image
import sys

d = "$DEPLOY_DIR"

try:
    icon = Image.open(f"{d}/Android/StoreListing/Icon/icon_512x512.png")
    if icon.size == (512, 512):
        print(f"[OK] Store icon dimensions: {icon.size[0]}x{icon.size[1]}")
    else:
        print(f"[WARN] Store icon dimensions are {icon.size[0]}x{icon.size[1]}, expected 512x512")
except Exception as e:
    print(f"[WARN] Could not verify icon: {e}")

try:
    fg = Image.open(f"{d}/Android/StoreListing/FeatureGraphic/feature_graphic_1024x500.png")
    if fg.size == (1024, 500):
        print(f"[OK] Feature graphic dimensions: {fg.size[0]}x{fg.size[1]}")
    else:
        print(f"[WARN] Feature graphic dimensions are {fg.size[0]}x{fg.size[1]}, expected 1024x500")
except Exception as e:
    print(f"[WARN] Could not verify feature graphic: {e}")
PY

echo ""
if [[ ${#ISSUES[@]} -eq 0 ]]; then
    echo "========================================"
    echo " ✅ DEPLOY PACKAGE VALID"
    echo "========================================"
    exit 0
else
    echo "========================================"
    echo " ❌ DEPLOY PACKAGE INVALID"
    echo " Issues found: ${#ISSUES[@]}"
    echo "========================================"
    for issue in "${ISSUES[@]}"; do
        echo "  - $issue"
    done
    exit 1
fi
