pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "analytics-platform"

include(":backend")
include(":sdk:analytics-kmp")
include(":sdk:analytics-compose")
include(":samples:android-app")
