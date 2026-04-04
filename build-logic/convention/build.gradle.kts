plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("hermesAndroidApplication") {
            id = "hermes.android.application"
            implementationClass = "HermesAndroidApplicationPlugin"
        }
        register("hermesAndroidLibrary") {
            id = "hermes.android.library"
            implementationClass = "HermesAndroidLibraryPlugin"
        }
        register("hermesKotlinLibrary") {
            id = "hermes.kotlin.library"
            implementationClass = "HermesKotlinLibraryPlugin"
        }
        register("hermesHilt") {
            id = "hermes.hilt"
            implementationClass = "HermesHiltPlugin"
        }
    }
}
