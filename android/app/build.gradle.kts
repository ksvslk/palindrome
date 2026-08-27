import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

android {
    namespace = "com.indest.hah"
    compileSdk = 36
    // --- GIT COMMIT ID CONFIG ---
    // Safely retrieves the commit ID without breaking the Configuration Cache
    val gitCommitId = providers.of(GitCommitValueSource::class) {}.get()

    defaultConfig {
        applicationId = "com.indest.hah"
        minSdk = 24
        targetSdk = 36
        versionCode = 3618
        versionName = "7.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GIT_COMMIT_ID", "\"$gitCommitId\"")

    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    configurations.all {
        exclude(group = "com.google.android.gms", module = "play-services-ads")
        exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
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

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.hilt.android)
    implementation(libs.play.services.games.v2)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.purchases)

    // --- FIREBASE ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)

    implementation(libs.play.services.measurement.api)
    implementation(libs.androidx.room.common.jvm)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Additional AndroidX libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Material Design
    implementation(libs.google.android.material)
    implementation(libs.androidx.gridlayout)

    implementation(libs.ads.mobile.sdk)

    implementation(libs.konfetti.xml)

    implementation(libs.review)
    implementation(libs.review.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.truth)
    testImplementation(libs.podam)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// --- HELPER CLASS ---
// Must remain at the bottom or top level of the file
abstract class GitCommitValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        return try {
            val output = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                standardOutput = output
            }
            String(output.toByteArray(), Charset.defaultCharset()).trim()
        } catch (e: Exception) {
            "dev"
        }
    }
}

tasks.register<Copy>("copyReleaseFilesToOneDirectory") {
    // 1. Define Source and Destination using parentheses
    from(
        layout.buildDirectory.file("outputs/bundle/instantRelease/app-instant-release.aab"),
        layout.buildDirectory.file("outputs/bundle/regularRelease/app-regular-release.aab")
    )
    into(layout.buildDirectory.dir("outputs/bundle/"))

    // 2. Define Dependencies inside the block
    // We use a list here to keep it clean, applying dependsOn and mustRunAfter to each
    val dependentTasks = listOf(
        ":app:produceRegularReleaseBundleIdeListingFile",
        ":app:produceInstantReleaseBundleIdeListingFile",
        ":app:bundleRelease"
    )

    dependentTasks.forEach { taskPath ->
        dependsOn(taskPath)
        mustRunAfter(taskPath)
    }
}