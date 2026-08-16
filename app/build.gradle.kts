plugins {

    id("com.android.application")

    id("org.jetbrains.kotlin.android")

}



val appName = providers.gradleProperty("appName").orElse("IM Any Builder").get()

val versionNameValue = providers.gradleProperty("versionName").orElse("1.0.0").get()

val versionCodeValue = providers.gradleProperty("versionCode").orElse("1").get().toInt()

val packageNameValue = providers.gradleProperty("packageName").orElse("com.imanybuilder.app").get()

val orientationValue = providers.gradleProperty("orientation").orElse("Auto").get()

val webViewModeValue = providers.gradleProperty("webViewMode").orElse("Offline").get()

val imBrandingValue = providers.gradleProperty("imBranding").orElse("mandatory").get()

require(orientationValue in setOf("Portrait", "Landscape", "Auto")) { "orientation must be Portrait, Landscape, or Auto" }

require(webViewModeValue in setOf("Online", "Offline")) { "webViewMode must be Online or Offline" }

require(imBrandingValue == "mandatory") { "IM branding is mandatory for every build" }

val manifestOrientation = when (orientationValue) {

    "Portrait" -> "portrait"

    "Landscape" -> "landscape"

    else -> "unspecified"

}



android {

    namespace = "com.imanybuilder.app"

    compileSdk = 35



    defaultConfig {

        applicationId = packageNameValue

        minSdk = 23

        targetSdk = 35

        versionCode = versionCodeValue

        versionName = versionNameValue

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "WEB_VIEW_MODE", "\"$webViewModeValue\"")

        buildConfigField("String", "IM_BRANDING", "\"$imBrandingValue\"")

    }



    buildTypes {

        debug {

            applicationIdSuffix = ".debug"

            versionNameSuffix = "-debug"

        }

        release {

            val storeFilePath = System.getenv("ANDROID_KEYSTORE_PATH")

            val storePasswordValue = System.getenv("ANDROID_KEYSTORE_PASSWORD")

            val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS")

            val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD")

            val hasSigning = listOf(storeFilePath, storePasswordValue, keyAliasValue, keyPasswordValue).all { !it.isNullOrBlank() }



            if (hasSigning) {

                signingConfig = signingConfigs.create("release") {

                    storeFile = file(storeFilePath!!)

                    storePassword = storePasswordValue

                    keyAlias = keyAliasValue

                    keyPassword = keyPasswordValue

                }

            } else {

                println("Release signing secrets are absent; release build will fail with a clear message.")

            }

            isMinifyEnabled = false

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        }

    }



    lint {

        abortOnError = true

    }

}



dependencies {

    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.activity:activity-ktx:1.9.2")

    implementation("androidx.webkit:webkit:1.12.1")

}



android.defaultConfig.manifestPlaceholders["appLabel"] = appName

android.defaultConfig.manifestPlaceholders["screenOrientation"] = manifestOrientation

