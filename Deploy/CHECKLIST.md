# EverTask — Google Play Launch Checklist

Use this checklist to ensure nothing is missed before hitting "Submit for review."

---

## Phase 1: Account & Prerequisites

- [ ] **Google Play Developer account** created and paid ($25 one-time)
- [ ] **Release keystore** generated and backed up securely
- [ ] **Environment variables** or `local.properties` configured with keystore credentials
- [ ] **Privacy policy URL** ready (host `Shared/Legal/privacy_policy.md` publicly)
- [ ] **Support email** set up (recommend: `support@evertask.app`)

---

## Phase 2: Build Validation

- [ ] Run `.\Scripts\validate_deploy.ps1` → must pass with ✅ `DEPLOY PACKAGE VALID`
- [ ] Install and test `Android/Build/Output/evertask-1.0.0-debug.apk` on a physical Android device
- [ ] Confirm release AAB exists: `Android/Build/Output/evertask-1.0.0-release.aab`
- [ ] Confirm ProGuard mapping files exist in `Android/Build/Output/mapping/`

---

## Phase 3: Google Play Console Setup

### App Creation
- [ ] Create new app in Play Console
- [ ] App name: **EverTask**
- [ ] Default language: **English (United States)**
- [ ] App or game: **App**
- [ ] Free or paid: **Free** (change later if monetizing)

### Store Listing
- [ ] Upload `Android/StoreListing/Icon/icon_512x512.png` as Store Icon
- [ ] Upload `Android/StoreListing/FeatureGraphic/feature_graphic_1024x500.png` as Feature Graphic
- [ ] Upload **2-8 phone screenshots** (follow specs in `Screenshots/README.md`)
- [ ] Upload **0-8 tablet screenshots** (7-inch and/or 10-inch, optional but recommended)
- [ ] Copy `title.txt` into **Short title** (30 chars max) and **App name**
- [ ] Copy `short_description.txt` into **Short description** (80 chars max)
- [ ] Copy `full_description.txt` into **Full description**
- [ ] Set category: **Productivity**
- [ ] Set tags: `Task Management`, `To Do List`, `Productivity`, `Voice Assistant`

### Contact Details
- [ ] Website: `https://evertask.app` (or your landing page)
- [ ] Email: `support@evertask.app`

---

## Phase 4: Compliance Forms

### Data Safety
- [ ] Open `Android/Policy/data_safety_form.md`
- [ ] Answer every question exactly as specified in that file
- [ ] Confirm: **No data collected**, **No data shared**, **Encryption at rest = Yes**, **Encryption in transit = No** (offline app)

### Content Rating (IKO)
- [ ] Open `Android/Policy/content_rating_guide.md`
- [ ] Answer the questionnaire
- [ ] Expected result: **PEGI 3** / **ESRB Everyone** / **Everyone**

### Privacy Policy
- [ ] Paste the hosted privacy policy URL into the Play Console field
- [ ] Ensure the URL is publicly accessible without login

---

## Phase 5: App Content

- [ ] **App access:** Select "All functionality is available without special access"
- [ ] **Ads:** Select "No, my app does not contain ads"
- [ ] **Content ratings:** Complete and save
- [ ] **Target audience:** Select **18+ only** (or change if targeting younger users, which requires COPPA compliance)
- [ ] **News apps:** Select "No"
- [ ] **COVID-19:** Select "No"
- [ ] **Data deletion:** Link to `Shared/Legal/data_deletion_policy.md` or explain uninstall deletes all data

---

## Phase 6: Release Setup

- [ ] Navigate to **Production** → **Create new release**
- [ ] Upload `evertask-1.0.0-release.aab`
- [ ] Release name: `1.0.0`
- [ ] Release notes: "Initial release of EverTask — voice-powered task breakdown with smart templates, widgets, and exact-alarm reminders."
- [ ] Save

---

## Phase 7: Reviewer Information

- [ ] Open `Review/ReviewNotes.txt`
- [ ] Paste the notes into the **App review** → **Notes for the review team** field
- [ ] Contact email: `support@evertask.app`
- [ ] Phone number: (add if required by your region)

---

## Phase 8: Pre-Submission Final Check

- [ ] All sections in Play Console show a green checkmark ✅
- [ ] No policy warnings or errors
- [ ] AAB file size looks reasonable (~5-15 MB expected)
- [ ] Screenshots are current and reflect the actual UI
- [ ] Store icon and feature graphic are crisp and not blurry
- [ ] You have backed up your keystore in at least 2 secure locations

---

## Phase 9: Submit

- [ ] Click **Send for review** (or **Start rollout to Production** if already approved)
- [ ] Note the submission date and time
- [ ] Set a calendar reminder to check status in 3-5 days

---

## Phase 10: Post-Submission

- [ ] Check email daily for review updates
- [ ] Monitor the Play Console dashboard for policy issues
- [ ] Respond to any reviewer questions within 24 hours
- [ ] After approval, announce the launch using `Marketing/press_release.md`

---

## Emergency Contacts

- **Google Play Developer Support:** https://support.google.com/googleplay/android-developer
- **Google Play Console Help:** https://support.google.com/googleplay/android-developer/?hl=en#topic=3450769
