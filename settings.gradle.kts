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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // 🔥 Đảm bảo JitPack được khai báo trong repositories
        maven("https://repo.eclipse.org/content/repositories/paho-releases/")// Repository cho Paho MQTT
    }
}

rootProject.name = "Weather2"
include(":app")
 