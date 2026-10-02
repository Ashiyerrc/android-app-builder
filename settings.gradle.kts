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
        maven("https://jitpack.io")
    }
}

rootProject.name = "android-app-builder"

// Core modules
include(":core")
include(":core:common")
include(":core:design-system")
include(":core:ui")

// Feature modules
include(":feature")
include(":feature:app-builder")
include(":feature:code-generator")
include(":feature:ui-designer")
include(":feature:project-management")

// App modules
include(":app")
include(":app:mobile")

// CLI and tools
include(":tools")
include(":tools:cli")
include(":tools:code-gen-plugin")

// Testing
include(":testing")
