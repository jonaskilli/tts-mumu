plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.github.jing332.tts"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

configurations.testImplementation {
    exclude(module = "logback-android")
}
dependencies {
    coreLibraryDesugaring(libs.desugar)
    api(project(":lib-database"))
    api(project(":lib-script"))
    api(project(":lib-compose"))

    implementation(project(":lib-common"))

    implementation(libs.hutool.cache)

    implementation(libs.logging)
    implementation(libs.slf4j.api)
//    implementation(libs.slf4j.simple)
    implementation(libs.logback.android)
    testImplementation(libs.logback.classic)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coreKtx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}