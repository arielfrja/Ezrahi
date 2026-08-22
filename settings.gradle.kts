pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "Ezrahi"
include(":app")
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:network")
include(":core:location")
include(":core:map-engine")
