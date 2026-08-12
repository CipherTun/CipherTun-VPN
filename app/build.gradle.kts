import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.triplet.play")
    alias(libs.plugins.spotless)
}

fun getProps(propName: String): String {
    val propsInEnv = System.getenv("LOCAL_PROPERTIES")

    if (propsInEnv != null) {
        val props = Properties()
        props.load(
            ByteArrayInputStream(
                Base64.getDecoder().decode(propsInEnv)
            )
        )

        val value = props.getProperty(propName)

        if (value != null) {
            return value
        }
    }

    val propsFile = rootProject.file("local.properties")

    if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))

        val value = props.getProperty(propName)

        if (value != null) {
            return value
        }
    }

    return ""
}

fun getVersionProps(propName: String): String {
    val propsFile = rootProject.file("version.properties")

    if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))

        val value = props.getProperty(propName)

        if (value != null) {
            return value
        }
    }

    return ""
}

android {
    namespace = "io.surprise.ciphertun"

    compileSdk = 36

    ndkVersion = "28.2.13676358"

    System.getenv("ANDROID_NDK_HOME")?.let {
        ndkPath = it
    }

    ksp {
        arg("room.incremental", "true")
        arg("room.schemaLocation", "${projectDir}/schemas")
    }

    defaultConfig {
        applicationId = "io.surprise.ciphertun"

        minSdk = 21
        targetSdk = 35

        versionCode = getVersionProps("VERSION_CODE").toInt()
        versionName = getVersionProps("VERSION_NAME")

        base.archivesName.set(
            "CipherTun-VPN-${versionName}"
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storeType = "PKCS12"

            storePassword = getProps("KEYSTORE_PASS")
            keyAlias = getProps("ALIAS_NAME")
            keyPassword = getProps("ALIAS_PASS")
        }
    }

    buildTypes {
        debug {
            if (getProps("KEYSTORE_PASS").isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        release {
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            vcsInfo.include = false
        }
    }

    dependenciesInfo {
        includeInApk = false
    }

    flavorDimensions += "vendor"

    productFlavors {
        create("play") {
            minSdk = 23
        }

        create("other") {
            minSdk = 23
        }

        create("otherLegacy") {
            minSdk = 21
        }
    }

    sourceSets {
        getByName("play") {
            java.directories.add("src/minApi23/java")
            aidl.directories.add("src/minApi23/aidl")
        }

        getByName("other") {
            java.directories.addAll(
                listOf(
                    "src/minApi23/java",
                    "src/github/java"
                )
            )

            aidl.directories.add("src/minApi23/aidl")
        }

        getByName("otherLegacy") {
            java.directories.addAll(
                listOf(
                    "src/minApi21/java",
                    "src/github/java"
                )
            )

            aidl.directories.add("src/minApi23/aidl")
        }
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true

            reset()

            include(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        viewBinding = true
        aidl = true
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        fatal += "NewApi"
    }
}

/*
 * AGP 9.4 public Variant API.
 */
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                output.outputFileName.get()
                    .replace("-release", "")
                    .replace(
                        "-otherLegacy",
                        "-legacy-android-5"
                    )
                    .replace("-other", "")
            )
        }
    }
}