<div align="center">

kotlin-linesweeper
==================

**Kotlin bindings for @jneem's [linesweeper] library.**

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7f52ff.svg)](https://kotlinlang.org/)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)
[![Maven central](https://img.shields.io/maven-central/v/io.github.adrientetar/kotlin-linesweeper?color=brightgreen)](https://central.sonatype.com/artifact/io.github.adrientetar/kotlin-linesweeper)

</div>

[linesweeper] is a Rust library that implements robust boolean operations on closed Bézier paths.

This library provides Kotlin bindings to the linesweeper library using Mozilla's [UniFFI] toolchain.

Maven library
-------------

```kotlin
repositories {
    mavenCentral()
}

val linesweeperVersion = "1.0.0"
val linesweeperTarget = run {
    val osName = System.getProperty("os.name").lowercase()
    val archName = System.getProperty("os.arch").lowercase()

    val os = when {
        "mac" in osName || "darwin" in osName -> "macos"
        "windows" in osName -> "windows"
        else -> "linux"
    }
    val arch = when (archName) {
        "aarch64", "arm64" -> "arm64"
        else -> "x64"
    }
    "$os-$arch"
}

dependencies {
    implementation("io.github.adrientetar:kotlin-linesweeper:$linesweeperVersion")
    runtimeOnly("io.github.adrientetar:kotlin-linesweeper:$linesweeperVersion:$linesweeperTarget")
}
```

Usage
-----

```kotlin
import io.github.adrientetar.linesweeper.*

// Create a square
val square = BezierPath().apply {
    moveTo(0.0, 0.0)
    lineTo(10.0, 0.0)
    lineTo(10.0, 10.0)
    lineTo(0.0, 10.0)
    closePath()
}

// Create a heart
val heart = BezierPath().apply {
    moveTo(5.0, 7.5)
    curveTo(5.0, 5.0, 2.5, 2.5, 0.0, 5.0)  // Left curve
    curveTo(-2.5, 7.5, 0.0, 10.0, 5.0, 15.0)  // Left top
    curveTo(10.0, 10.0, 12.5, 7.5, 10.0, 5.0)  // Right top
    curveTo(7.5, 2.5, 5.0, 5.0, 5.0, 7.5)  // Right curve back to start
    closePath()
}

// Perform a union operation
val result = try {
    booleanOperation(
        square,
        heart,
        BooleanOperation.UNION,
        FillRule.EVEN_ODD,
    )
} catch (e: LineSweeperException.InfiniteInputException) {
    // Handle infinite coordinate values
} catch (e: LineSweeperException.NaNInputException) {
    // Handle NaN coordinate values
} catch (e: LineSweeperException.NonClosedPathException) {
    // Handle non-closed paths
}

println("Union result has ${result.paths.size} path(s)")
```

Development
-----------

To build this library, you need:

- [Rust](https://rustup.rs/) (latest stable version) with the `cargo` build tool in your PATH
- JDK 11 or higher

```bash
# build
./gradlew build

# Run Rust tests
cargo test

# Run Kotlin tests (after building bindings)
./gradlew test
```

[UniFFI]: https://mozilla.github.io/uniffi-rs/
[linesweeper]: https://github.com/jneem/linesweeper
