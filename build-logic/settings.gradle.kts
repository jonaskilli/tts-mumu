pluginManagement {
    repositories {
        // CI 环境直接用官方仓库, 本地用阿里云镜像加速
        if (!(System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true")) {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/central")
        }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        if (!(System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true")) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
            maven("https://maven.aliyun.com/repository/central")
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
