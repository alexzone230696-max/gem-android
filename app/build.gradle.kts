import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.Base64
import java.io.File

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    kotlin("android")
    kotlin("kapt")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.gemwallet.android"
    compileSdk = 36
    ndkVersion = "28.1.13356709"

    val channelDimension by extra("channel")
    flavorDimensions.add(channelDimension)

    defaultConfig {
        applicationId = "com.gemwallet.android"
        minSdk = 28
        targetSdk = 36
        versionCode = Integer.valueOf(System.getenv("BUILD_NUMBER") ?: "1")
        versionName = System.getenv("BUILD_VERSION") ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        splits {
            abi {
                isEnable = false
                include("arm64-v8a", "armeabi-v7a")
                isUniversalApk = false
            }
        }
    }

    productFlavors {
        create("google") { dimension = channelDimension }
        create("fdroid") { dimension = channelDimension }
        create("huawei") { dimension = channelDimension }
        create("solana") { dimension = channelDimension }
        create("universal") { dimension = channelDimension }
        create("samsung") { dimension = channelDimension }
    }

    signingConfigs {
        create("release") {
            val b64 = System.getenv("KEYSTORE_BASE64")
            val storeFileFromEnv = System.getenv("KEYSTORE_FILENAME")
            val keystoreFile: File = when {
                !storeFileFromEnv.isNullOrBlank() -> file(storeFileFromEnv)
                !b64.isNullOrBlank() -> {
                    val out = rootProject.file("release.keystore")
                    out.writeBytes(Base64.getDecoder().decode(b64))
                    out
                }
                else -> file("release.keystore")
            }

            storeFile = keystoreFile
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_ALIAS_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            if (System.getenv("UNIT_TESTS") == "true") {
                ndk {
                    abiFilters.remove("arm64-v8a")
                    abiFilters.remove("armeabi-v7a")
                    abiFilters.add("x86_64")
                }

                splits {
                    abi {
                        reset()
                        isEnable = false
                        include("x86_64")
                        isUniversalApk = false
                    }
                }
            }

            buildConfigField("String", "TEST_PHRASE", "${System.getenv("TEST_PHRASE")}")
        }

        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            val skipSign = System.getenv("SKIP_SIGN") == "true"
            val flavorEnv = System.getenv("FLAVOR")?.lowercase()
            signingConfig = if (skipSign || flavorEnv == "fdroid") null else signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += "META-INF/*"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs { useLegacyPackaging = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kapt { correctErrorTypes = true }
    androidResources { generateLocaleConfig = true }
    room { schemaDirectory("$projectDir/schemas") }

    lint {
        disable += "Instantiatable"
        checkGeneratedSources = true
        checkDependencies = true
    }
}

// **Фильтруем задачи сборки, чтобы собирать только universal**
tasks.whenTaskAdded {
    if ((name.contains("assemble", ignoreCase = true) || name.contains("bundle", ignoreCase = true)) &&
        !name.lowercase().contains("universal")
    ) {
        enabled = false
    }
}

dependencies {
    implementation(project(":blockchain"))
    implementation(project(":ui"))
    implementation(project(":data:repositories"))

    // Features
    implementation(project(":features:activities:presents"))
    implementation(project(":features:activities:viewmodels"))
    implementation(project(":features:add_asset:presents"))
    implementation(project(":features:add_asset:viewmodels"))
    implementation(project(":features:asset:presents"))
    implementation(project(":features:asset:viewmodels"))
    implementation(project(":features:asset_select:presents"))
    implementation(project(":features:asset_select:viewmodels"))
    implementation(project(":features:banner:presents"))
    implementation(project(":features:banner:viewmodels"))
    implementation(project(":features:buy:presents"))
    implementation(project(":features:buy:viewmodels"))
    implementation(project(":features:confirm:presents"))
    implementation(project(":features:confirm:viewmodels"))
    implementation(project(":features:transfer_amount:presents"))
    implementation(project(":features:transfer_amount:viewmodels"))
    implementation(project(":features:swap:presents"))
    implementation(project(":features:swap:viewmodels"))
    implementation(project(":features:receive:presents"))
    implementation(project(":features:receive:viewmodels"))
    implementation(project(":features:wallets:presents"))
    implementation(project(":features:wallets:viewmodels"))
    implementation(project(":features:earn:stake:presents"))
    implementation(project(":features:earn:stake:viewmodels"))
    implementation(project(":features:earn:delegation:presents"))
    implementation(project(":features:earn:delegation:viewmodels"))
    implementation(project(":features:settings:aboutus:presents"))
    implementation(project(":features:settings:aboutus:viewmodels"))
    implementation(project(":features:settings:currency:presents"))
    implementation(project(":features:settings:currency:viewmodels"))
    implementation(project(":features:settings:develop:presents"))
    implementation(project(":features:settings:develop:viewmodels"))
    implementation(project(":features:settings:networks:presents"))
    implementation(project(":features:settings:networks:viewmodels"))
    implementation(project(":features:settings:price_alerts:presents"))
    implementation(project(":features:settings:price_alerts:viewmodels"))
    implementation(project(":features:settings:security:presents"))
    implementation(project(":features:settings:security:viewmodels"))
    implementation(project(":features:settings:settings:presents"))
    implementation(project(":features:settings:settings:viewmodels"))
    implementation(project(":features:recipient:presents"))
    implementation(project(":features:nft:presents"))
    implementation(project(":features:update_app:presents"))
    implementation(project(":features:wallet-details:presents"))
    implementation(project(":features:bridge:presents"))
    implementation(project(":features:bridge:viewmodels"))
    implementation(project(":features:assets:presents"))
    implementation(project(":features:assets:viewmodels"))

    implementation(libs.ktx.core)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.vico.m3)
    implementation(libs.reorderable)

    // Universal only
    "universalImplementation"(project(":flavors:fcm"))
    "universalImplementation"(project(":flavors:google-review"))

    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.junit.runner)
    testImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.uiautomator)
}