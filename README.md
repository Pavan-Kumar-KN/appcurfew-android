
<p align="center">
  <img src="https://assets.devsandboxhub.shop/appcurfew/app_curfew_logo.png" alt="AppCurfew Logo" width="120" />
</p>

<h1 align="center">AppCurfew</h1>

<p align="center">
  Take control of your screen time
</p>

# AppCurfew

AppCurfew is an Android app designed to help you regain control over your screen time by enforcing strict app usage boundaries during specific time periods.

## The Problem

Many of us intend to stop using distracting apps at night or during work hours, but end up scrolling anyway. Apps like YouTube, Instagram, and others are designed to keep us engaged, making self-control difficult—especially late at night.

Even when users set limits, they are easy to bypass.

## The Solution

AppCurfew enforces discipline by blocking selected apps during scheduled time windows. When a blocked app is opened, the user is immediately redirected back to the home screen, preventing usage.

This creates a friction layer between intention and distraction.

> Built for people who want control over their time when willpower is not enough.

## Features

- Enforce app blocking during custom time windows
- Prevent access to distracting apps (e.g. YouTube, social media)
- Support overnight schedules such as `22:00` to `06:00`
- Always allow essential apps like dialer and contacts
- Simple app selection with search support
- Lightweight and fully on-device (no data collection)

## Why This Exists

This project started as a personal attempt to reduce late-night screen time and regain focus during important hours.

## Project Setup

### Requirements

- Android Studio
- JDK 11 or newer
- An Android device or emulator running Android 7.0+ (`minSdk 24`)

### Build and Run

1. Open the project in Android Studio.
2. Let Gradle sync complete.
3. Run the app on a device or emulator.
4. Grant the accessibility permission in system settings when prompted.

### Enable Blocking

1. Open AppCurfew.
2. Turn on the blocking switch.
3. Pick the start and end times.
4. Open the app selection screen and choose the apps to block.
5. Enable the AppCurfew accessibility service if it is not already active.

## How It Works

AppCurfew stores the schedule, enabled state, and blocked package list in SharedPreferences. When the accessibility service detects a new foreground app, it checks whether blocking is enabled, whether the current time is inside the bedtime window, and whether the app is whitelisted. If the app should be blocked, the service sends the user back to the home screen.

## Allowed Apps

The following apps are always allowed during bedtime mode:

- `com.android.dialer`
- `com.android.contacts`
- `com.google.android.dialer`
- `com.android.phone`

## Permissions

AppCurfew requires the accessibility permission so it can monitor the foreground app and enforce blocking. The app also requests package visibility so it can show the installed apps list for selection.

## Notes

- Blocking is based on the selected apps list and the active schedule.
- Overnight schedules are supported.
- The project currently uses a home-screen redirect as the blocking action.

## Development

Useful files:

- [MainActivity](app/src/main/java/com/pavan/appcurfew/MainActivity.kt)
- [AppSelectionActivity](app/src/main/java/com/pavan/appcurfew/AppSelectionActivity.kt)
- [AppBlockAccessibilityService](app/src/main/java/com/pavan/appcurfew/AppBlockAccessibilityService.kt)
- [BedtimePrefs](app/src/main/java/com/pavan/appcurfew/BedtimePrefs.kt)

To build the app from the command line:

```bash
./gradlew :app:assembleDebug
```
