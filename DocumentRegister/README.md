# Document Register — Lab 3, variant 5

Java Android application for employee memoranda and applications awaiting signature.
Each record contains a document type, the employee's full name, and a document date.
SQLite sorts records by date ascending, then by ID for equal dates.

Open this directory in Android Studio Koala or newer. Configure an Android SDK
path in local.properties or ANDROID_HOME. Minimum device API: 30. Compile/target API: 37.
The application ID is com.g433_v5.lab3, so the earlier labs remain installed separately.

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon
```

The two activities provide creation, editing, single-record deletion and deletion
of all records, with confirmation dialogs. Dates use DD.MM.YYYY in the interface,
strict calendar validation and years 1900–2100. The database stores ISO dates.
The UI supports portrait/landscape and light/dark themes, system insets, keyboard
scrolling, a custom icon and author information.

RecyclerView and its AndroidX support components are integrated locally in app/libs;
there are no remote application library dependencies. See app/libs/NOTICE.md and
SHA256.json for licenses, versions and provenance. Gradle and Android build tools
still use their normal tooling repositories.

See [testing](TESTING.md). Report: [lab3-report.docx](../reports/lab3-report.docx).
