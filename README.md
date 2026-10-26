# Mobile Development Labs

Android laboratory projects written in Java.

## Lab 1: Pyramid surface area and volume

Project: [`Calculatingtheparametersofaregularpyramid`](Calculatingtheparametersofaregularpyramid/)

The app reads two base side lengths and a height, validates that all values are positive, and displays the surface area and volume. It models a right pyramid whose apex is above the center of a rectangular base; when the two base sides are equal, the pyramid is regular. The UI supports light and dark themes and portrait and landscape layouts.

Open the project directory in Android Studio (Koala or newer) and run the `app` configuration on an emulator with API 30 or newer. The project uses the included Gradle wrapper. No SDK path, build output, or IDE user state is committed.

## Lab 2: Magnetic field sensor (variant 5)

Project: [`MagneticFieldMonitor`](MagneticFieldMonitor/).

A separate Java application using only the Android framework (no external libraries). It displays live magnetometer X/Y/Z values in microteslas, field magnitude and accuracy, with start/stop controls, a full sensor inventory and author information. The listener is released while the activity is paused. Both screen orientations and light/dark themes are supported.

Open `MagneticFieldMonitor` in Android Studio (Koala or newer). Use an API 30+ device or an emulator with a magnetometer. The projects have different application IDs and can be installed together.

```powershell
cd MagneticFieldMonitor
.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon
```

See [testing instructions and results](MagneticFieldMonitor/TESTING.md).

Report: [Lab 2 report](reports/lab2-report.docx).
