import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing via omgevingsvariabelen (GitHub Actions) of keystore.properties
// (lokale build, staat in .gitignore). GEEN wachtwoorden in dit bestand!
val keystoreProperties: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { props -> Properties().apply { props.inputStream().use { load(it) } } }

fun signingValue(env: String, propKey: String): String? =
    System.getenv(env) ?: keystoreProperties?.getProperty(propKey)

android {
    namespace = "nl.wissels.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "nl.wissels.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(
                signingValue("WISSELS_KEYSTORE_FILE", "storeFile") ?: "wissels.keystore"
            )
            storePassword = signingValue("WISSELS_STORE_PASSWORD", "storePassword")
            keyAlias = signingValue("WISSELS_KEY_ALIAS", "keyAlias")
            keyPassword = signingValue("WISSELS_KEY_PASSWORD", "keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signeren alleen als de keystore aanwezig is; anders een ongetekende release.
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile?.exists() == true }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

// APK's hernoemen naar Wissels-vX.X.apk (release) en Wissels-vX.X-debug.apk (debug),
// zodat je nooit hoeft te gissen welke versie je installeert.
// Lokaal leverbaar via: ./gradlew :app:WisselsApk   of   ./gradlew :app:WisselsDebugApk
// (de rename draait ook automatisch ná assembleDebug / assembleRelease).
val apkBaseName = "Wissels-v" + (android.defaultConfig.versionName ?: "0.0")

listOf("debug" to "-debug", "release" to "").forEach { (variant, suffix) ->
    val capitalized = variant.replaceFirstChar { it.uppercase() }
    val rename = tasks.register("rename${capitalized}Apk") {
        val apk = layout.buildDirectory.file("outputs/apk/$variant/app-$variant.apk")
        val target = layout.buildDirectory.file("outputs/apk/$variant/$apkBaseName$suffix.apk")
        doLast {
            val src = apk.get().asFile
            if (src.exists()) {
                src.copyTo(target.get().asFile, overwrite = true)
                logger.lifecycle("APK: ${target.get().asFile.name}")
            }
        }
    }
    tasks.matching { it.name == "assemble$capitalized" }.configureEach { finalizedBy(rename) }
}

tasks.register("WisselsApk") {
    dependsOn("assembleRelease", "renameReleaseApk")
}

tasks.register("WisselsDebugApk") {
    dependsOn("assembleDebug", "renameDebugApk")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
