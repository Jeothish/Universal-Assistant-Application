pluginManagement {
    repositories {
        google()

        mavenCentral()
        gradlePluginPortal()
        maven {url=uri("https://chaquo.com/maven-public")}
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
