pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )

    repositories {
        google()
        mavenCentral()

        // JitPack dependencies:
        // - libsu 6.0.0
        // - compose-markdown 0.5.8
        maven {
            url = uri("https://jitpack.io")
        }

        // Legacy Xposed API 82 repository.
        maven {
            url = uri("https://artifactory.appodeal.com/appodeal-public/")
        }
    }
}

rootProject.name = "sing-box"

include(":app")

include(":libxposed-api")
project(":libxposed-api").projectDir =
    file("third_party/libxposed-api")

include(":terminal-emulator")
project(":terminal-emulator").projectDir =
    file("third_party/termux-app/terminal-emulator")

include(":terminal-view")
project(":terminal-view").projectDir =
    file("third_party/termux-app/terminal-view")