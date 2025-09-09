package examples

import io.github.adrientetar.linesweeper.*

fun main() {
    println("kotlin-linesweeper - Basic example")
    println("==================================")

    // Example 1: Union of square and circle
    println("\n1. Union of square and circle:")
    unionExample()

    // Example 2: Intersection of square and circle
    println("\n2. Intersection of square and circle:")
    intersectionExample()

    // Example 3: Difference operation (circle with square hole)
    println("\n3. Difference operation (circle with square hole):")
    differenceExample()

    // Example 4: XOR operation (square and circle)
    println("\n4. XOR operation (square and circle):")
    xorExample()
}

fun unionExample() {
    // Create a square
    val square = createSquare(0.0, 0.0, 1.0)

    // Create a circle overlapping with the square
    val circle = createCircle(0.5, 0.5, 0.6)

    try {
        val result = booleanOperation(
            square,
            circle,
            BooleanOperation.UNION,
            FillRule.EVEN_ODD,
        )

        println("   Original: square + circle")
        println("   Result: ${result.paths.size} contour(s)")
        println("   Union creates a single merged shape")

    } catch (e: Exception) {
        println("   Error: ${e.message}")
    }
}

fun intersectionExample() {
    // Create a square
    val square = createSquare(0.0, 0.0, 2.0)

    // Create a circle
    val circle = createCircle(1.0, 1.0, 1.2)

    try {
        val result = booleanOperation(
            square,
            circle,
            BooleanOperation.INTERSECTION,
            FillRule.EVEN_ODD,
        )

        println("   Square intersected with circle")
        println("   Result: ${result.paths.size} contour(s)")
        println("   Intersection creates the overlapping area")

    } catch (e: Exception) {
        println("   Error: ${e.message}")
    }
}

fun differenceExample() {
    // Create a large circle
    val outerCircle = createCircle(0.0, 0.0, 2.0)

    // Create a smaller square inside it (this will be subtracted)
    val innerSquare = createSquare(-1.0, -1.0, 2.0)

    try {
        val result = booleanOperation(
            outerCircle,
            innerSquare,
            BooleanOperation.DIFFERENCE,
            FillRule.EVEN_ODD,
        )

        println("   Large circle with smaller square subtracted")
        println("   Result: ${result.paths.size} contour(s)")
        println("   Creates a circle with a square hole")

    } catch (e: Exception) {
        println("   Error: ${e.message}")
    }
}

fun xorExample() {
    // Create a square
    val square = createSquare(0.0, 0.0, 2.0)

    // Create a circle that overlaps with the square
    val circle = createCircle(1.5, 1.5, 1.0)

    try {
        val result = booleanOperation(
            square,
            circle,
            BooleanOperation.XOR,
            FillRule.EVEN_ODD,
        )

        println("   XOR of square and circle")
        println("   Result: ${result.paths.size} contour(s)")
        println("   Creates the non-overlapping parts only")

    } catch (e: Exception) {
        println("   Error: ${e.message}")
    }
}

fun createSquare(x: Double, y: Double, size: Double): BezierPath {
    val path = BezierPath()
    path.moveTo(x, y)
    path.lineTo(x + size, y)
    path.lineTo(x + size, y + size)
    path.lineTo(x, y + size)
    path.closePath()
    return path
}

fun createCircle(centerX: Double, centerY: Double, radius: Double): BezierPath {
    val path = BezierPath()

    // Magic number for cubic Bezier circle approximation
    val controlDistance = radius * 0.552284749831

    // Start at the top of the circle
    path.moveTo(centerX, centerY - radius)

    // Top-right arc
    path.curveTo(
        centerX + controlDistance, centerY - radius,
        centerX + radius, centerY - controlDistance,
        centerX + radius, centerY,
    )

    // Right-bottom arc
    path.curveTo(
        centerX + radius, centerY + controlDistance,
        centerX + controlDistance, centerY + radius,
        centerX, centerY + radius,
    )

    // Bottom-left arc
    path.curveTo(
        centerX - controlDistance, centerY + radius,
        centerX - radius, centerY + controlDistance,
        centerX - radius, centerY,
    )

    // Left-top arc
    path.curveTo(
        centerX - radius, centerY - controlDistance,
        centerX - controlDistance, centerY - radius,
        centerX, centerY - radius,
    )

    path.closePath()
    return path
}
