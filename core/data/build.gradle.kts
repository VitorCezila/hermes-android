plugins {
    id("hermes.android.library")
    id("hermes.hilt")
}

android {
    namespace = "com.cezila.hermes.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:domain"))
}
