plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.indest.hah"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        minSdk = 23
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // This benchmark buildType is used for benchmarking, and should function like your
        // release build (for example, with minification on). It's signed with a debug key
        // for easy local/CI testing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("instantRelease", "regularRelease")
            proguardFiles("benchmark-rules.pro.txt")
        }
    }

    flavorDimensions += "default"
    productFlavors {
        create("instant") {
            dimension = "default"
        }
        create("regular") {
            dimension = "default"
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.junit.v121)
    implementation(libs.androidx.espresso.core.v361)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4.v132)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.appcompat.v170)
}

androidComponents {
    beforeVariants(selector().all()) {
        enable = buildType == "benchmark"
    }
}
