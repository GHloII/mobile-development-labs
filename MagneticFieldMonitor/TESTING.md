# Lab 2 testing

Variant 5: magnetic field. Java, minSdk 30, compile/target SDK 37. No external runtime dependencies.

## Verified environment

Pixel_8a AVD, Android 17 / API 37, x86_64. Goldfish 3-axis Magnetic field sensor (vendor: The Android Open Source Project), maximum range 2000 microteslas, resolution 0.5 microteslas. This is a simulated sensor; physical magnetometer accuracy/calibration has not been measured.

## Build and checks

`./gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon` succeeded. Lint reported no errors; remaining warnings concern the backup API, redundant layout parents and background overdraw.

## Emulator checks

Build the APK and boot one emulator with a magnetic-field sensor. Run `python scripts/verify_emulator.py` (Python standard library only). The script installs the debug APK, changes the emulator sensor values, operates the app, saves UI dumps/screenshots to ignored `app/build/verification`, and asserts:

| Scenario | Expected and observed |
| --- | --- |
| Field 3, 4, 12 | X/Y/Z match, magnitude 13.00 microteslas |
| Field -6, 8, 0 | Signed X preserved, magnitude 10.00 |
| Zero field | Magnitude 0.00 |
| Stop then change field | Readings stay frozen; stopped status shown |
| Rotate while stopped | Session remains stopped |
| Start again | Latest field received, measuring status shown |
| Sensor inventory | Sensor count and parameter cards shown; Back returns to readings |
| Author dialog | Names from lab 1 cover shown |
| Portrait/landscape and light/dark | Text and readings visible; longer content scrolls |
| Background and return | Listener released in onPause, data resumes on return |

The script changes emulator rotation, theme and magnetic field for these tests; it finishes in portrait/light mode with field 3, 4, 12.

The no-magnetometer and failed-registration branches are implemented but were not exercised on this AVD, which exposes the sensor. They should be checked on a device without a magnetometer; the app must show a diagnostic, disable measurement when absent, and still open the sensor list. This does not require location or camera permissions.
