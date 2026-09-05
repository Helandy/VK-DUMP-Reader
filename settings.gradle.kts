pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VK DUMP Reader"
include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:navigation")
include(":core:storage")
include(":core:archive")
include(":core:settings")
include(":core:security")
include(":core:update")
include(":features:home")
include(":features:importer")
include(":features:dialogs")
include(":features:profile")
include(":features:chat")
include(":features:favorites")
include(":features:settings")
include(":features:lock")
