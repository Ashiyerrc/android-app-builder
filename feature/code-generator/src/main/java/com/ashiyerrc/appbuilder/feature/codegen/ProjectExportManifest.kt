package com.ashiyerrc.appbuilder.feature.codegen

import com.squareup.kotlinpoet.*

data class ProjectExportManifest(
    val appName: String,
    val packageName: String,
    val version: String = "1.0.0",
    val screens: List<ScreenManifestEntry>,
    val navigationGraph: NavigationGraphSpec,
    val dependencies: List<String>,
    val metadata: Map<String, String> = emptyMap()
)

data class ScreenManifestEntry(
    val screenName: String,
    val route: String,
    val filePath: String,
    val stateModel: String,
    val parameters: List<NavParameter> = emptyList()
)

data class NavParameter(
    val name: String,
    val type: String,
    val required: Boolean = false
)

data class NavigationGraphSpec(
    val startRoute: String,
    val destinations: List<NavDestination>,
    val transitions: List<NavTransition> = emptyList()
)

data class NavDestination(
    val route: String,
    val screenName: String,
    val parameters: List<NavParameter> = emptyList()
)

data class NavTransition(
    val from: String,
    val to: String,
    val trigger: String
)

class ProjectExportManifestBuilder {
    private var appName: String = "MyApp"
    private var packageName: String = "com.example.myapp"
    private var version: String = "1.0.0"
    private val screens = mutableListOf<ScreenManifestEntry>()
    private val navigationDestinations = mutableListOf<NavDestination>()
    private val navigationTransitions = mutableListOf<NavTransition>()
    private val dependencies = mutableListOf<String>()
    private val metadata = mutableMapOf<String, String>()

    fun appName(name: String) = apply { this.appName = name }
    fun packageName(pkg: String) = apply { this.packageName = pkg }
    fun version(ver: String) = apply { this.version = ver }

    fun addScreen(entry: ScreenManifestEntry) = apply { screens.add(entry) }
    fun addNavDestination(dest: NavDestination) = apply { navigationDestinations.add(dest) }
    fun addNavTransition(transition: NavTransition) = apply { navigationTransitions.add(transition) }
    fun addDependency(dep: String) = apply { dependencies.add(dep) }
    fun addMetadata(key: String, value: String) = apply { metadata[key] = value }

    fun build(): ProjectExportManifest {
        return ProjectExportManifest(
            appName = appName,
            packageName = packageName,
            version = version,
            screens = screens,
            navigationGraph = NavigationGraphSpec(
                startRoute = navigationDestinations.firstOrNull()?.route ?: "splash",
                destinations = navigationDestinations,
                transitions = navigationTransitions
            ),
            dependencies = dependencies,
            metadata = metadata
        )
    }
}
