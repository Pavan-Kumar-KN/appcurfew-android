# AppCurfew

AppCurfew is an Android bedtime blocker that lets you pick specific apps to restrict during a scheduled time window. It uses an Accessibility Service to detect the foreground app and immediately sends the user back home when a blocked app is opened during the active schedule.

## Features

- Block selected apps during a bedtime window
- Support overnight schedules such as `22:00` to `06:00`
- Always allow dialer and contacts
- Save settings locally with SharedPreferences
- Search installed apps before selecting which ones to block
- Prompt the user to enable the accessibility service when needed

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
