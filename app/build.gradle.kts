import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val adsConf = Properties().apply {
    rootProject.file("ads.conf").inputStream().use { load(it) }
}
fun conf(key: String, default: String): String = adsConf.getProperty(key, default).trim()

android {
    namespace = "com.alameen.fullquran"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alameen.fullquran"
        minSdk = 24
        targetSdk = 34
        val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = run
        versionName = "1.0.$run"

        manifestPlaceholders["admobAppId"] =
            conf("admob_app_id", "ca-app-pub-3940256099942544~3347511713")

        buildConfigField("String", "ADS_FORMAT", "\"${conf("format", "banner")}\"")
        buildConfigField("String", "UNIT_FIRST", "\"${conf("unit_first", "ca-app-pub-3940256099942544/8691691433")}\"")
        buildConfigField("String", "UNIT_RETURN", "\"${conf("unit_return", "ca-app-pub-3940256099942544/1033173712")}\"")
        buildConfigField("String", "UNIT_BANNER", "\"${conf("unit_banner", "ca-app-pub-3940256099942544/9214589741")}\"")
        buildConfigField("String", "UNIT_APP_OPEN", "\"${conf("unit_app_open", "ca-app-pub-3940256099942544/9257395921")}\"")
        buildConfigField("int", "RETURN_MIN_GAP_SEC", conf("return_min_gap_sec", "20"))
        buildConfigField("long", "FIRST_WAIT_MS", conf("first_wait_ms", "5000") + "L")
        buildConfigField("String", "AUDIO_BASE_URL", "\"${conf("audio_base_url", "https://github.com/Aminu8191/quran-app/releases/download/audio-v2/")}\"")
        buildConfigField("String", "REMOTE_CONFIG_URL", "\"${conf("remote_config_url", "")}\"")
    }

    signingConfigs {
        create("release") {
            val ks = System.getenv("KEYSTORE_FILE")
            if (ks != null && File(ks).exists()) {
                storeFile = File(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (System.getenv("KEYSTORE_FILE") != null)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("m4a", "opus", "ogg")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")

    implementation("com.google.android.gms:play-services-ads:23.4.0")
}
