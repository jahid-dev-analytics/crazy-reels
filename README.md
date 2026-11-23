# Crazy Reels

Counts the reels and shorts you scroll, shows a brain that gets more tired as the count climbs, and lets you set a daily limit.

| Folder | What it is | Status |
|---|---|---|
| `android/` | Android app (Kotlin + Jetpack Compose) | Phase 0: home screen with brain preview |
| `web/` | Website and dashboard (Cloudflare Pages) | Not started |
| `worker/` | API (Cloudflare Worker + D1) | Not started |
| `extension/` | Chrome extension for laptops | Not started |

Plan: https://claude.ai/artifact/BFUwPaYSNLrAh1QMhLrn3B

## Build the Android app

Needs JDK 17+ and the Android SDK (installed by Android Studio's setup wizard).

```bash
cd android
./gradlew assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/app-debug.apk`. Install it on a phone with USB debugging on:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Or open the `android` folder in Android Studio and press Run.
