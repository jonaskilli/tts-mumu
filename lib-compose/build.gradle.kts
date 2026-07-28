plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.github.jing332.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()

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

    android {
        packaging {
            resources {
                excludes += setOf("META-INF/INDEX.LIST", "META-INF/*.md")
            }
        }
    }
}

dependencies {
    api(project(":lib-common"))

    api(libs.bundles.coil)
    implementation(libs.bundles.markwon)
    implementation(libs.bundles.accompanist)

    val composeBom = platform(libs.compose.bom)
//    def composeBom = platform("dev.chrisbanes.compose:compose-bom:2024.01.00-alpha01")
    api(composeBom)
    androidTestApi(composeBom)
    api(libs.bundles.compose)
    api(libs.bundles.compose.floatingx)
    api(libs.bundles.compose.material3)
    api(libs.webkit)
    api(libs.localbroadcast)

    androidTestApi("androidx.compose.ui:ui-test-junit4")
    debugApi("androidx.compose.ui:ui-test-manifest")
    api("androidx.compose.ui:ui-tooling-preview")
    debugApi("androidx.compose.ui:ui-tooling")

    implementation(libs.coreKtx)
    implementation(libs.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}