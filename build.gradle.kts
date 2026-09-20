// Top-level build file. Module-specific configuration lives in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    // Firebase: declared here, applied in :app once google-services.json is added.
    alias(libs.plugins.google.services) apply false
}
