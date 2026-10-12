plugins { alias(libs.plugins.android.application) }

android {
    namespace = "com.example.barrioalerta"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.barrioalerta"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
}
