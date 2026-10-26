plugins { id("com.android.application") }
android {
    namespace = "com.g433_v5.lab2"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.g433_v5.lab2"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
