pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Kotlin/JS & Kotlin/Wasm register Node.js/Yarn/Binaryen Ivy repositories on projects
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        google()
        mavenCentral()
    }
}

rootProject.name = "LogicLabs-Web"
include(":core-digital")
include(":core-bridge")
include(":core-data")
include(":core-designsystem")
include(":feature-breadboard")
include(":feature-instruments")
include(":feature-tools")
include(":core-testing")
include(":shared-app")
include(":desktop-app")
include(":web-app")
