import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Release signing secrets are read from keystore.properties (git-ignored, lives only on the build machine) or from
// SONARA_* environment variables (GitHub Actions secrets). Nothing secret is ever written into the repository.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(property: String, env: String): String? =
    (keystoreProps.getProperty(property) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sonara.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonara.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 12
        versionName = "1.0.0-m7"
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "SONARA_KEYSTORE_FILE")
        val storePassword = signingValue("storePassword", "SONARA_KEYSTORE_PASSWORD")
        val keyAlias = signingValue("keyAlias", "SONARA_KEY_ALIAS")
        val keyPassword = signingValue("keyPassword", "SONARA_KEY_PASSWORD")
        // Only defined when every value is present, so a machine without the keystore still configures fine
        // and simply produces an unsigned release.
        if (storeFile != null && storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                this.storeFile = rootProject.file(storeFile).takeIf { it.exists() } ?: file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }

        // A debug-signed build with the optimisations of a release build (R8 shrinking and optimisation,
        // not debuggable). Debug builds run Compose unoptimised and feel noticeably less smooth than
        // the real app, so scrolling and animation are judged on this build. It installs next to the
        // debug build under its own application id.
        create("perf") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".perf"
            versionNameSuffix = "-perf"
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkDependencies = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    // Exported Room schemas are committed so migrations can be tested explicitly.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.animation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.guava)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
