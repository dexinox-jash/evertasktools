# Privacy Policy for EverTask

**Last Updated:** April 14, 2026

## Introduction

EverTask ("we," "our," or "us") is committed to protecting your privacy. This Privacy Policy explains how our mobile application ("EverTask" or the "App") handles information.

**Our core promise: EverTask is designed to work entirely offline. We do not collect, transmit, or store your personal data on our servers.**

## Information We Do Not Collect

EverTask does not collect or transmit any of the following:
- Personal identifiers (name, email, phone number)
- Location data
- Usage analytics or crash reports to external servers
- Advertising identifiers
- Cloud account credentials

All data created within the App remains on your device unless you explicitly choose to share it.

## Information Stored on Your Device

The App stores the following information locally on your device:
- **Task data:** Task titles, subtasks, completion status, duration estimates, and sort order
- **Templates:** Pre-built and user-created task templates
- **Backup files:** Optional JSON backup files exported to your device storage

This data is stored in a local SQLite database within the App's private storage area.

## Voice Input (RECORD_AUDIO)

EverTask allows you to create tasks using your voice. When you use this feature:
- Audio is processed using the device's built-in speech recognition system (Android SpeechRecognizer)
- Voice data is **not recorded, stored, or transmitted** by EverTask
- Voice processing occurs locally or through your device's standard speech recognition services
- You can deny microphone permission and continue using the App with manual text input

## Notifications (POST_NOTIFICATIONS)

EverTask sends local notifications to remind you about scheduled tasks. These notifications:
- Are generated entirely on your device
- Do not require an internet connection
- Do not transmit any data to external servers
- Can be disabled through your device settings or by denying notification permission

## Alarms and Scheduling (SCHEDULE_EXACT_ALARM)

EverTask uses your device's alarm system to schedule precise task reminders. This permission:
- Is used solely for local reminder scheduling
- Does not involve any data transmission
- Can be managed through your device settings

## Boot Scheduling (RECEIVE_BOOT_COMPLETED)

EverTask uses this permission solely to reschedule local reminders after your device reboots. This permission:
- Does not read or transmit any personal data
- Is required so reminders persist across reboots
- Can be disabled by revoking the permission in device settings, though reminders may not appear after a reboot

## Haptic Feedback (VIBRATE)

EverTask uses this permission to provide haptic feedback when you press buttons in the app. This permission:
- Is used only for local tactile feedback
- Does not involve any data collection or transmission

## Background Work (WAKE_LOCK, FOREGROUND_SERVICE, ACCESS_NETWORK_STATE)

These permissions are implicitly required by Android WorkManager, which EverTask uses for reliable background task scheduling. These permissions:
- Are used only to ensure reminders are delivered reliably
- Do not transmit any data to external servers
- Are standard system-level permissions managed automatically by the Android framework

## Data Retention and Deletion

Since all data is stored locally on your device:
- Data persists only as long as the App remains installed
- Uninstalling the App permanently deletes all local data
- You can also delete individual tasks or clear all data within the App at any time

## Backup Files

The App can create optional JSON backup files in your device's external storage (`/Android/data/com.evertask.app/files/EverTask/`). These files:
- Are created only when you use the backup feature
- Remain under your control
- Are not uploaded to any cloud service by EverTask

## Children's Privacy

EverTask is not directed at children under the age of 13. We do not knowingly collect any information from children.

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be posted within the App and reflected in the "Last Updated" date above.

## Contact Us

If you have any questions or concerns about this Privacy Policy, please contact us at:

**Email:** support@evertask.app
