plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}

// Support for Termux development: Use local AAPT2 if running in Termux environment
val termuxAapt2 = file("/data/data/com.termux/files/home/android-sdk/build-tools/34.0.4/aapt2")
if (termuxAapt2.exists()) {
    allprojects {
        extra.set("android.aapt2FromMavenOverride", termuxAapt2.absolutePath)
    }
}
