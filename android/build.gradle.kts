import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

// The website contains the same public Supabase endpoint and publishable key.
// Local properties can override them for a development project.
val publicCloudConfig = rootProject.file("website/config.js").readText()
fun publicCloudValue(name: String): String =
    Regex("""\b$name:\s*["']([^"']+)["']""").find(publicCloudConfig)?.groupValues?.get(1)
        ?: error("Missing $name in website/config.js")

android {
    namespace = "org.openswim.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.openswim.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-test"
        resValue("string", "supabase_url", localProperties.getProperty("supabase.url", publicCloudValue("url")))
        resValue("string", "supabase_publishable_key", localProperties.getProperty("supabase.publishableKey", publicCloudValue("publishableKey")))
    }
    buildFeatures { compose = true; resValues = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3:1.4.0")
}
