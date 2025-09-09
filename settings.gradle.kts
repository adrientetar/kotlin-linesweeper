
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.22")
            version("jna", "5.13.0")

            version("truth", "1.4.1")

            version("mavenPublish", "0.34.0")
        }
    }
}

rootProject.name = "kotlin-linesweeper"
