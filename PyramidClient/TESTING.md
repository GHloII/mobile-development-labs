# Lab 4 verification

Run from the project directory after building and starting the local HTTPS/PHP
server as documented in README. Use one disposable emulator.

```
python server/verify_service.py
python scripts/verify_emulator.py
```

The server script verifies the real TLS certificate against the generated CA and
checks four known answers and twelve invalid protocol/input cases. Results are
written to ignored `app/build/verification/service-results.json`.

The Android script installs the debug APK, fills fields through ADB/UI Automator,
checks actual PHP answers, empty/zero/oversized dimensions, HTTP URL and query
rejection, TLS failure, HTTP error, malformed JSON, read timeout, rotation during
a request, retry, all theme/orientation combinations and author information.
It temporarily runs a separate localhost HTTPS fixture for error responses only.
Screenshots, fresh XML hierarchies and `ui-results.json` are saved in that directory.

Environment: Windows, Pixel_8a AVD, Android 17 / API 37, x86_64; PHP 8.4.27.
Build/lint command: `gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon`.
Android Lint has no errors; warnings are not claimed absent. No physical phone or
public production server was tested. The default server requires the local process
and ADB reverse; without them the app shows a connection error.
