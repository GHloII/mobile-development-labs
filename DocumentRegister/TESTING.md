# Lab 3 verification

Build and static checks:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon
```

Runtime verification uses an API 37 Pixel_8a emulator and the standard Python library:

```powershell
python scripts/verify_emulator.py
```

The script installs the debug APK and **clears only com.g433_v5.lab3 data** before
running. Use a disposable emulator installation, not one containing real documents.
It exercises the UI through ADB/UI Automator and reads a copy of the debug app's
SQLite file using run-as to verify that each operation has the expected effect.
Results, UI hierarchies and screenshots are written to app/build/verification
(ignored by Git).

Coverage:

- Empty state; empty and whitespace-only employee names.
- Empty/invalid dates, non-leap February 29, strict format and year limits.
- Valid leap day and inclusive year boundaries 1900 and 2100.
- Inserting both document types, chronological sorting and equal-date ID ordering.
- Editing with stable ID, a quoted name and a changed date that reorders the list.
- Draft restoration after rotation and cancellation without saving.
- Single deletion from both screens, cancellation of deletion, complete deletion.
- Data persistence after force-stop and relaunch.
- Portrait/landscape, light/dark themes, date picker and author information.

Limitations: verification uses an emulator, not a physical phone. SQLite corruption,
storage exhaustion and future schema migrations are not simulated. SQLiteException
handlers are present. The sample data left after the run is fictional and entered
in Latin characters because ADB input text does not support Cyrillic text.
