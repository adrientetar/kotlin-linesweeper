plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("java-library")
    id("com.vanniktech.maven.publish") version libs.versions.mavenPublish
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
    sourceSets {
        main {
            kotlin.srcDirs("bindings")
        }
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:" + libs.versions.jna.get())

    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:" + libs.versions.truth.get())
}

tasks.test {
    useJUnitPlatform()
}

// Custom tasks for building Rust library and generating Kotlin bindings
tasks.register<Exec>("buildRustLibrary") {
    description = "Build the Rust library"
    commandLine("cargo", "build", "--release")
    workingDir = projectDir
}

tasks.register<Exec>("generateKotlinBindings") {
    description = "Generate Kotlin bindings using UniFFI"
    dependsOn("buildRustLibrary")

    val outputDir = file("bindings")
    outputs.dir(outputDir)

    doFirst {
        outputDir.mkdirs()
    }

    commandLine(
        "cargo", "run", "--features=uniffi/cli", "--bin", "uniffi-bindgen", "generate",
        "--library", "target/release/libkotlin_linesweeper.dylib",
        "--language", "kotlin",
        "--config", "uniffi.toml",
        "--out-dir", "bindings"
    )
    workingDir = projectDir
}

tasks.register<Copy>("copyNativeLibrary") {
    description = "Copy the built native library to resources"
    dependsOn("buildRustLibrary")

    val libName = when {
        System.getProperty("os.name").lowercase().contains("windows") -> "kotlin_linesweeper.dll"
        System.getProperty("os.name").lowercase().contains("mac") -> "libkotlin_linesweeper.dylib"
        else -> "libkotlin_linesweeper.so"
    }

    from("target/release/$libName")
    into("src/main/resources")
}

tasks.register("prepareKotlin") {
    description = "Prepare Kotlin sources and native libraries"
    dependsOn("generateKotlinBindings", "copyNativeLibrary")
}

tasks.named("compileKotlin") {
    dependsOn("prepareKotlin")
}

tasks.named("processResources") {
    dependsOn("copyNativeLibrary")
}

// Configure JAR to include native libraries
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/resources") {
        include("*.so", "*.dll", "*.dylib")
    }
}

afterEvaluate {
    tasks.named("sourcesJar") {
        dependsOn("generateKotlinBindings", "copyNativeLibrary")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}
