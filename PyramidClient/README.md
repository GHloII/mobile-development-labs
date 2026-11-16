# Lab 4 — HTTPS pyramid calculator, variant 5

Java Android client and PHP calculation service. The assignment reuses the lab 1
geometry: a pyramid with its apex above the centre of a rectangular base.
It is regular when `a == b`. The **PHP server** calculates:

`S = a*b + a*hypot(h,b/2) + b*hypot(h,a/2)` and `V = a*b*h/3`.

The Android app only validates, sends a POST, parses JSON and displays the answer.
It has no external runtime dependencies. Java 11 source, min SDK 30, compile/target
37, the repository's Gradle 9.6 / AGP 9.4.1 setup. Both themes and orientations,
system/keyboard insets, resource strings, own vector icon and authors are included.

## Local demonstration

PHP 8.4+ and Python 3 with `cryptography` are needed on the development computer.
Use official PHP binaries: <https://www.php.net/downloads.php?os=windows>.
Python is used only for development TLS setup and the gateway, not by Android.

1. `python server/create_dev_certificate.py` creates a fresh local certificate,
   private server key in ignored `server/.local`, and public debug trust anchor.
2. Build **after** generating the certificate:
   `.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon`.
3. `python server/serve_dev.py --php C:/path/to/php.exe` starts real PHP on host
   loopback 8084 and a TLS gateway on host loopback 8443. Leave it running.
4. `adb reverse tcp:8443 tcp:8443` connects emulator/device localhost to host.
5. Install/run `app/build/outputs/apk/debug/app-debug.apk`, keep the default
   `https://localhost:8443/solve.php`, enter `6, 6, 4`, calculate: `96`, `48`.

The gateway forwards only `/solve.php`. It does not implement the mathematics.
Nothing is deployed publicly by these steps. Regenerating the certificate requires
rebuilding/reinstalling the debug APK. Never commit `.local` private keys.

## HTTPS and deployment

Cleartext traffic is disabled. No permissive hostname verifier or trust manager is
installed. The **debug** build trusts the local development CA only for `localhost`.
The **release** source set contains no extra trust anchor/configuration and uses
normal system certificate trust. For a physical phone, `adb reverse` also works
over USB; otherwise deploy `solve.php` behind Apache/Nginx with a valid publicly
trusted certificate, then enter its HTTPS URL in the app. Deploy only the PHP
endpoint, not the local development scripts/key directory. A public server/account
is not supplied by the coursework repository.

## API

POST `/solve.php`, `Content-Type: application/x-www-form-urlencoded`, fields `a`,
`b`, `h`: finite numbers, `0 < value <= 1000000`. Decimal dot in the protocol;
the app also normalizes a typed decimal comma. Unit choice is consistent with input.

Success: `{"ok":true,"res":{"surface":96,"volume":48}}`.
Errors use JSON with `ok:false` and a code; HTTP 405/415/422 reject invalid methods,
content types or dimensions. Invalid/missing/array/infinite values are rejected
server-side too. Positive results that underflow floating-point storage are rejected.

The app uses an executor with connect/read timeouts of 8 seconds, disables
redirects, caps response size, validates the JSON result and resets its button
after errors. Rotation saves fields and completed output; an in-flight request is
cancelled and a retry message is shown. It does not silently recalculate locally.

See [test results](TESTING.md) and [report](../reports/lab4-report.docx).
