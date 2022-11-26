plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.firebase.firebase-perf")
    id("androidx.navigation.safeargs")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.android")
}

android {

    namespace = "com.hover.stax"

    compileSdk = 33

    defaultConfig {
        applicationId = "com.hover.stax"
        minSdk = 21
        targetSdk = 33
        versionCode = 202
        versionName = "1.18.3"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
    }

    signingConfigs {
        register("releaseConfig") {
            keyAlias = providers.gradleProperty("keyAlias").orNull
            keyPassword = providers.gradleProperty("keyPassword").orNull
            storeFile = providers.gradleProperty("storeFile").orNull?.let { file(it) }
            storePassword = providers.gradleProperty("storePassword").orNull
        }
    }

    buildTypes {
        getByName("debug") {
            withGroovyBuilder {
                "FirebasePerformance" {
                    invokeMethod("setInstrumentationEnabled", false)
                }
            }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("releaseConfig")
        }
    }

    bundle {
        language {
            //Ensures all language string resources is bundled in the aab.
            enableSplit = false
        }
    }

    lint {
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("debug").assets.srcDirs(files("$projectDir/schemas"))
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "../hover.sdk", "include" to listOf("*.jar", "*.aar"))))

    // Google
    implementation(libs.bundles.google)
    kapt(libs.lifecycle.common)

    //compose
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.tooling)
    androidTestImplementation(libs.compose.ui.test)

    //logging
    implementation(libs.bundles.logging)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:31.0.1"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-inappmessaging-display")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.play:core:1.10.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Auth
    implementation(libs.auth)

    // Networking
    implementation(libs.bundles.network)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.release)

    implementation(libs.libphonenumber)
    implementation(libs.lingver)

    // Images
    implementation(libs.bundles.image)
    kapt(libs.glide.compiler)

    // Room
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    // DI
    implementation(libs.bundles.koin)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.junit.androidx)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.room.test)

    // Hover SDK
    debugImplementation(project(":hover.sdk"))
    debugImplementation(libs.bundles.hover)
    releaseImplementation(libs.hover)
}