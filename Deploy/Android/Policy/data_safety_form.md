# Google Play Data Safety Form — Copy-Paste Answers

Use this document to fill out the Google Play Console Data Safety section. Since EverTask is entirely offline and local-first, almost every answer is "No."

---

## Overview

- **Does your app collect or share any of the required user data types?** → **No**
- **Is all user data your app collects encrypted in transit?** → **No** (data never leaves the device)

---

## Data Collection & Sharing

### Does your app collect any of the following data types?

For **every** data type listed below, select **"No"** (unchecked):

- [ ] Location
- [ ] Personal info (name, email, address, phone number, etc.)
- [ ] Financial info (credit card, bank account, etc.)
- [ ] Health and fitness
- [ ] Messages (emails, SMS, in-app messages, etc.)
- [ ] Photos and videos
- [ ] Audio files
- [ ] Files and docs
- [ ] Calendar
- [ ] Contacts
- [ ] App activity (app interactions, in-app search history, installed apps, other user-generated content, other actions)
- [ ] Web browsing
- [ ] App info and performance (crash logs, diagnostics, other app performance data)
- [ ] Device or other IDs

**What to paste in Play Console if a free-text field asks for explanation:**
> EverTask does not collect any user data. All task data, templates, and backups are stored locally on the user's device. The app functions entirely offline and does not transmit data to external servers.

---

## Data Usage & Handling

### Does your app share any user data with third parties?
- **No**

### Does your app transfer user data to other countries or territories?
- **No** (no data is transferred anywhere)

### Does your app use data for advertising or marketing purposes?
- **No**

### Does your app use data for fraud prevention?
- **No**

### Does your app use data for personalization?
- **No**

### Does your app use data for app functionality?
- **Yes** — but only locally. The app stores tasks, subtasks, and templates in a local SQLite database on the device. No data is sent off-device.

**Explanation to paste:**
> Data is stored locally to provide core app functionality such as task management, reminders, and template expansion. All processing happens on the device.

---

## Data Security

### Is your app data encrypted in transit?
- **No**
- **Reason:** The app does not transmit user data over any network. All data remains on the user's device.

### Is your app data encrypted at rest?
- **Yes**
- **Details to paste:**
> User data is stored in the app's private SQLite database, which is protected by Android's sandbox and filesystem encryption (AES-256 when device encryption is enabled).

### Has your app undergone an independent security review?
- **No**

---

## User Rights & Deletion

### Can users request deletion of their data?
- **Yes**
- **Method to paste:**
> Users can delete all data within the app (task-by-task or via clear-data), through Android Settings → Apps → EverTask → Clear data, or by uninstalling the app entirely.

### Can users access their data?
- **Yes**
- **Method to paste:**
> All data is visible and editable within the app. Users can also export a JSON backup to their device storage at any time.

### Can users correct their data?
- **Yes**
- **Method to paste:**
> Users can edit task titles, subtasks, and templates directly within the app.

### Can users export their data?
- **Yes**
- **Method to paste:**
> Users can create a JSON backup file in their device's external storage for portability.

---

## Compliance

### GDPR
- **Compliant** — Basis: App does not collect personal data. All data remains on the user's device.

### CCPA
- **Compliant** — Basis: App does not sell personal information. No personal information is collected.

### COPPA
- **Compliant** — Basis: App does not knowingly collect personal information from children under 13.

---

## Final Checklist Before Submitting

- [ ] "Does your app collect or share any of the required user data types?" is set to **No**
- [ ] All individual data types are unchecked
- [ ] "Encrypted in transit" is **No** with explanation
- [ ] "Encrypted at rest" is **Yes**
- [ ] Deletion, access, correction, and portability are all answered **Yes**
