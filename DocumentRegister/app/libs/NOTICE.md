# Integrated AndroidX components

Copyright The Android Open Source Project. Licensed under the Apache License 2.0.
See LICENSE-2.0.txt. The binaries are unmodified Google Maven artifacts.

RecyclerView 1.2.1 and its transitive support components are included locally
to satisfy the lab requirement to integrate external widgets in the project.
There are no remote application dependencies in app/build.gradle.kts.
Gradle and Android build tools still require their normal SDK/tooling installation.

Artifacts: androidx.recyclerview:recyclerview:1.2.1,
androidx.core:core:1.3.2, androidx.customview:customview:1.0.0,
androidx.versionedparcelable:versionedparcelable:1.1.0,
androidx.lifecycle:lifecycle-runtime:2.0.0,
androidx.lifecycle:lifecycle-common:2.0.0,
androidx.arch.core:core-common:2.0.0,
androidx.collection:collection:1.0.0, androidx.annotation:annotation:1.1.0.

Source and provenance: https://android.googlesource.com/platform/frameworks/support/
and https://dl.google.com/dl/android/maven2/
File checksums are recorded in SHA256.json.
