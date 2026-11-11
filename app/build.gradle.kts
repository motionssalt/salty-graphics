plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.motionssalt.saltygraphics"
    compileSdk = 35

    val envVersionCode = (System.getenv("APP_VERSION_CODE") ?: project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
    val envVersionName = (System.getenv("APP_VERSION_NAME") ?: project.findProperty("appVersionName") as String?) ?: "1.0.0-beta"

    defaultConfig {
        applicationId = "com.motionssalt.saltygraphics"
        minSdk = 26
        targetSdk = 35
        versionCode = envVersionCode
        versionName = envVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: "saltygraphics"
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "salty"
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "saltygraphics"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.forkEvery = 1 }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Android integration shell: all widgets, layouts and navigation belong to :ui-kit.
    implementation(project(":ui-kit"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
}
