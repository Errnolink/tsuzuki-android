plugins {
    id("mihon.library")
    id("mihon.library.compose")
    kotlin("android")
}

android {
    namespace = "dev.errnolink.tsuzuki.designsystem"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

dependencies {
    api(compose.foundation)
    api(compose.animation)
    implementation(androidx.corektx)
    implementation(libs.haze)
    implementation(libs.backdrop)
    debugImplementation(compose.activity)
}
