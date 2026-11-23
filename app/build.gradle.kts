
plugins {
    alias(libs.plugins.android.application)
}

val sampleAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val sampleBannerId = "ca-app-pub-3940256099942544/6300978111"
val releaseAdMobAppId = providers.gradleProperty("adMobAppId").orElse("").get().trim()
val releaseBannerId = providers.gradleProperty("adMobBannerId").orElse("").get().trim()
require(releaseAdMobAppId.isEmpty() == releaseBannerId.isEmpty()) {
    "Configure both adMobAppId and adMobBannerId, or leave both blank."
}

android {
    namespace = "crosswordtime.magicapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "crosswordtime.magicapp"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.gradleProperty("playVersionCode").orElse("8").get().toInt()
        versionName = "1.1"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            manifestPlaceholders["adMobAppId"] = sampleAdMobAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$sampleBannerId\"")
        }
        release {
            manifestPlaceholders["adMobAppId"] = releaseAdMobAppId.ifEmpty { sampleAdMobAppId }
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$releaseBannerId\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        )
    }
}

dependencies {
    implementation("com.android.billingclient:billing:9.0.0")
    implementation("com.google.android.gms:play-services-ads:24.9.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
