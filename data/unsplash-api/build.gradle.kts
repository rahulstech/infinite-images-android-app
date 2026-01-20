import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    kotlin("android")
}

android {
    namespace = "rahulstech.android.data.unsplash"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        // load unsplash api credentials from local.properties
        val localProperties = Properties()
        val localPropertiesFile = File("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }

        // first turn on adding custom build config field
        buildFeatures {
            buildConfig = true
        }

        // then add the following fields in BuildConfig.java file
        buildConfigField("String", "UNSPLASH_API_VERSION", "\"${localProperties.getProperty("UNSPLASH_API_VERSION") ?: "v1"}\"")

        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"${localProperties.getProperty("UNSPLASH_ACCESS_KEY") ?: ""}\"")
        // Secret Key is only required for making requests for unsplash user
        // buildConfigField("String", "UNSPLASH_SECRET_KEY", "\"${localProperties.getProperty("UNSPLASH_SECRET_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {

    // retrofit
    api(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // paging
    api(libs.paging.runtime)
    api(libs.paging.runtime.ktx)

    // testing
    testImplementation(libs.junit)
    implementation(kotlin("stdlib-jdk8"))
}