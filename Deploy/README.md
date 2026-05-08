# 🚀 EverTask — Google Play Publication Package

**Open this first.** This folder contains literally everything you need to publish EverTask on the Google Play Store.

---

## 📦 What's Inside

| Folder | What's There | Why It Matters |
|--------|--------------|----------------|
| `Android/Build/Output/` | **Signed release AAB**, debug APK, ProGuard mapping files | The actual binaries you upload to Google Play |
| `Android/StoreListing/` | Store icon (512x512), feature graphic (1024x500), screenshot specs, title, descriptions | Everything for the store listing page |
| `Android/Policy/` | Data safety form answers, content rating guide | Required Google Play compliance forms |
| `Review/` | Reviewer notes, demo script, contact info, response templates | Helps reviewers approve you faster |
| `Scripts/` | `build_android.ps1`, `validate_deploy.ps1` | Build the app and verify this package is complete |
| `Shared/Legal/` | Privacy policy, Terms of Service, Data deletion policy, EULA | Host the privacy policy on a public URL before submission |
| `Marketing/` | Press release, social asset specs | Launch day marketing materials |

---

## ⚡ Quick Start: 3 Steps to Publish

### 1. Validate the Package
Open PowerShell and run:
```powershell
.\Scripts\validate_deploy.ps1
```
If you see ✅ `DEPLOY PACKAGE VALID`, you're good. If anything is missing, the script will tell you exactly what.

### 2. Get a Release Keystore
If you don't have one, follow `Android/Build/keystore_setup.md` to create it.
Then set these environment variables (or add them to `kmp/local.properties`):
```powershell
$env:RELEASE_KEYSTORE_PATH = "C:\path\to\evertask-release.keystore"
$env:RELEASE_KEYSTORE_PASSWORD = "your_keystore_password"
$env:RELEASE_KEY_ALIAS = "evertask"
$env:RELEASE_KEY_PASSWORD = "your_key_password"
```

### 3. Upload to Google Play
1. Go to [Google Play Console](https://play.google.com/console)
2. Create a new app → name: **EverTask**
3. Upload `Android/Build/Output/evertask-1.0.0-release.aab` to Production
4. Fill the store listing using the files in `Android/StoreListing/`
5. Complete the Data Safety form using `Android/Policy/data_safety_form.md`
6. Complete Content Rating using `Android/Policy/content_rating_guide.md`
7. Add reviewer notes from `Review/ReviewNotes.txt`
8. Paste your hosted privacy policy URL (host `Shared/Legal/privacy_policy.md` publicly)
9. Submit for review

---

## 🧪 Need to Rebuild?

Run the build script:
```powershell
.\Scripts\build_android.ps1
```

This will:
- Run all unit tests
- Build the release AAB
- Copy the new artifacts back into `Android/Build/Output/`

---

## 📋 Before You Submit

Check `CHECKLIST.md` for the complete pre-flight checklist. The critical ones:
- [ ] You have a Google Play Developer account ($25 one-time fee)
- [ ] You have a release keystore
- [ ] Privacy policy is hosted on a public URL
- [ ] `validate_deploy.ps1` passes
- [ ] You've tested the debug APK on a real device

---

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| AAB upload fails with signing error | Check your keystore path and passwords in environment variables |
| Missing screenshots | Follow the exact specs in `Android/StoreListing/Screenshots/README.md` |
| Data Safety form confusion | Open `Android/Policy/data_safety_form.md` — every question has a direct answer |
| Play Console rejects privacy policy | Host the markdown file as HTML on any public URL (GitHub Pages, your domain, etc.) |

---

**Status:** Package built and validated on 2026-04-14  
**App Version:** 1.0.0 (versionCode 1)  
**Target:** Google Play Store — Android only (iOS scoped out)
