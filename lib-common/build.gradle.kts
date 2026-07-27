plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
//    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.github.jing332.lib_common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    android {
        packaging {
            resources {
                excludes += setOf("META-INF/INDEX.LIST", "META-INF/*.md")
            }
        }
    }
}

dependencies {
    api(libs.splitties.appctx)
    api(libs.hutool.crypto)
    api(libs.bundles.network)
    api(libs.bundles.media3)
    api(libs.kotlin.result)
    api(libs.kotlinx.serialization.json)
    api(libs.apache.commons.text)
    api(libs.logging)

    implementation(libs.coreKtx)
    implementation(libs.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}