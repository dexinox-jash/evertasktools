# Deploy Folder Index

Complete index of every file in the Deploy directory. Last updated: 2026-04-14.

---

## Root Files

| File | Purpose | Status |
|------|---------|--------|
| `README.md` | Start here — 3-step publish guide | ✅ Real file |
| `CHECKLIST.md` | Step-by-step Google Play submission checklist | ✅ Real file |
| `FINAL_PREFLIGHT_REPORT.md` | Complete GO/NO-GO validation report | ✅ Real file |
| `INDEX.md` | This file — complete inventory | ✅ Real file |

---

## Android/Build/Output/

| File | Purpose | Status |
|------|---------|--------|
| `evertask-1.0.0-release.aab` | Signed Android App Bundle for Google Play upload | ✅ Real artifact |
| `evertask-1.0.0-debug.apk` | Debug APK for device testing | ✅ Real artifact |
| `mapping/mapping.txt` | ProGuard deobfuscation mapping | ✅ Real artifact |
| `mapping/configuration.txt` | R8/ProGuard configuration output | ✅ Real artifact |
| `mapping/resources.txt` | Shrunk resources report | ✅ Real artifact |
| `mapping/seeds.txt` | Kept classes report | ✅ Real artifact |
| `mapping/usage.txt` | Usage analysis report | ✅ Real artifact |

---

## Android/StoreListing/

| File | Purpose | Status |
|------|---------|--------|
| `title.txt` | App title for Play Console | ✅ Real file |
| `short_description.txt` | 80-character store hook | ✅ Real file |
| `full_description.txt` | Full ASO-optimized description | ✅ Real file |
| `Icon/icon_512x512.png` | High-res store icon | ✅ Real image |
| `FeatureGraphic/feature_graphic_1024x500.png` | 1024x500 promo banner | ✅ Real image |
| `Screenshots/README.md` | Exact phone screenshot specs & capture guide | 📋 Spec |
| `Screenshots-7inch/README.md` | 7" tablet screenshot specs | 📋 Spec |
| `Screenshots-10inch/README.md` | 10" tablet screenshot specs | 📋 Spec |

---

## Android/Policy/

| File | Purpose | Status |
|------|---------|--------|
| `data_safety_form.md` | Copy-paste answers for Google Play Data Safety form | ✅ Real file |
| `data_safety_labels.json` | Machine-readable data safety reference | ✅ Real file |
| `content_rating_guide.md` | Answers for IKO content rating questionnaire | ✅ Real file |

---

## Android/Build/

| File | Purpose | Status |
|------|---------|--------|
| `build_instructions.md` | Step-by-step build guide from source to AAB | ✅ Real file |
| `keystore_setup.md` | How to create and secure a release keystore | ✅ Real file |

---

## Review/

| File | Purpose | Status |
|------|---------|--------|
| `ReviewNotes.txt` | Notes for Google Play review team | ✅ Real file |
| `DemoVideoScript.txt` | 30-second promotional video script | ✅ Real file |
| `ContactInfo.txt` | Developer contact template | ✅ Real file |
| `TestAccounts.txt` | Credentials for reviewers (N/A) | ✅ Real file |
| `SubmissionChecklist.txt` | Final pre-submit sanity check | ✅ Real file |
| `ReviewResponseTemplates.txt` | Response templates for reviews | ✅ Real file |

---

## Scripts/

| File | Purpose | Status |
|------|---------|--------|
| `build_android.ps1` | Windows PowerShell build script | ✅ Real script |
| `build_android.sh` | Bash build script | ✅ Real script |
| `validate_deploy.ps1` | Windows validation script | ✅ Real script |
| `validate_deploy.sh` | Bash validation script | ✅ Real script |

---

## Shared/Legal/

| File | Purpose | Status |
|------|---------|--------|
| `privacy_policy.md` | Privacy policy — host publicly before submission | ✅ Real file |
| `terms_of_service.md` | Terms of Service | ✅ Real file |
| `data_deletion_policy.md` | Data deletion instructions | ✅ Real file |
| `eula.md` | End User License Agreement | ✅ Real file |

---

## Marketing/

| File | Purpose | Status |
|------|---------|--------|
| `press_release.md` | Launch press release | ✅ Real file |
| `social_assets/README.md` | Specs for social media launch graphics | 📋 Spec |

---

## Quick Navigation by Task

| Task | Relevant Files |
|------|----------------|
| **First Time Setup** | `README.md`, `CHECKLIST.md` |
| **Build the App** | `Scripts/build_android.ps1`, `Android/Build/build_instructions.md` |
| **Upload to Play Store** | `Android/Build/Output/evertask-1.0.0-release.aab` |
| **Fill Store Listing** | `Android/StoreListing/*.txt`, `Android/StoreListing/Icon/`, `Android/StoreListing/FeatureGraphic/` |
| **Fill Compliance Forms** | `Android/Policy/data_safety_form.md`, `Android/Policy/data_safety_labels.json`, `Android/Policy/content_rating_guide.md` |
| **Reviewer Communication** | `Review/ReviewNotes.txt`, `Review/ContactInfo.txt` |
| **Legal Compliance** | `Shared/Legal/privacy_policy.md` (must be hosted publicly) |
| **Launch Marketing** | `Marketing/press_release.md` |
