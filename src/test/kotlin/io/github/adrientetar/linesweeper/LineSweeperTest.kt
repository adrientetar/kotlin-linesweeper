package io.github.adrientetar.linesweeper

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class LineSweeperTest {

    @Test
    fun testBasicUnionOperation() {
        // Create a simple square
        val square = BezierPath()
        square.moveTo(0.0, 0.0)
        square.lineTo(1.0, 0.0)
        square.lineTo(1.0, 1.0)
        square.lineTo(0.0, 1.0)
        square.closePath()

        // Create a circle overlapping with the square
        val circle = createCircle(0.5, 0.5, 0.6)

        // Perform union operation
        val result = booleanOperation(
            square,
            circle,
            BooleanOperation.UNION,
            FillRule.EVEN_ODD
        )

        assertThat(result).isNotNull()
        assertThat(result.paths).isNotEmpty()

        // The union of overlapping square and circle should result in one contour
        assertThat(result.paths).hasSize(1)
    }

    @Test
    fun testIntersectionOperation() {
        // Create a square
        val square = BezierPath()
        square.moveTo(0.0, 0.0)
        square.lineTo(2.0, 0.0)
        square.lineTo(2.0, 2.0)
        square.lineTo(0.0, 2.0)
        square.closePath()

        // Create a circle that overlaps with the square
        val circle = createCircle(1.0, 1.0, 1.2)

        val result = booleanOperation(
            square,
            circle,
            BooleanOperation.INTERSECTION,
            FillRule.EVEN_ODD
        )

        assertThat(result).isNotNull()
        assertThat(result.paths).isNotEmpty()
    }

    @Test
    fun testDifferenceOperation() {
        // Create a large circle
        val outerCircle = createCircle(0.0, 0.0, 2.0)

        // Create a smaller square inside it
        val innerSquare = BezierPath()
        innerSquare.moveTo(-1.0, -1.0)
        innerSquare.lineTo(1.0, -1.0)
        innerSquare.lineTo(1.0, 1.0)
        innerSquare.lineTo(-1.0, 1.0)
        innerSquare.closePath()

        val result = booleanOperation(
            outerCircle,
            innerSquare,
            BooleanOperation.DIFFERENCE,
            FillRule.EVEN_ODD
        )

        assertThat(result).isNotNull()
        assertThat(result.paths).isNotEmpty()

        // Should result in a circle with a square hole (represented as 2 contours)
        assertThat(result.paths).hasSize(2)
    }

    @Test
    fun testXorOperation() {
        // Create a square
        val square = BezierPath()
        square.moveTo(0.0, 0.0)
        square.lineTo(2.0, 0.0)
        square.lineTo(2.0, 2.0)
        square.lineTo(0.0, 2.0)
        square.closePath()

        // Create a circle that overlaps with the square
        val circle = createCircle(1.5, 1.5, 1.0)

        val result = booleanOperation(
            square,
            circle,
            BooleanOperation.XOR,
            FillRule.EVEN_ODD
        )

        assertThat(result).isNotNull()
        assertThat(result.paths).isNotEmpty()
    }


    @Test
    fun testQuadraticCurves() {
        // Test quadratic Bezier curves
        val quadPath = BezierPath()
        quadPath.moveTo(0.0, 0.0)
        quadPath.quadTo(1.0, 2.0, 2.0, 0.0)  // Parabolic curve
        quadPath.lineTo(2.0, 1.0)
        quadPath.lineTo(0.0, 1.0)
        quadPath.closePath()

        val rectangle = BezierPath()
        rectangle.moveTo(0.5, 0.0)
        rectangle.lineTo(1.5, 0.0)
        rectangle.lineTo(1.5, 1.5)
        rectangle.lineTo(0.5, 1.5)
        rectangle.closePath()

        val result = booleanOperation(
            quadPath,
            rectangle,
            BooleanOperation.INTERSECTION,
            FillRule.NON_ZERO
        )

        assertThat(result).isNotNull()
    }

    @Test
    fun testErrorHandling() {
        val validPath = BezierPath()
        validPath.moveTo(0.0, 0.0)
        validPath.lineTo(1.0, 0.0)
        validPath.lineTo(1.0, 1.0)
        validPath.lineTo(0.0, 1.0)
        validPath.closePath()

        // Test with NaN coordinates - should throw some form of LineSweeperException
        val nanPath = BezierPath()
        nanPath.moveTo(Double.NaN, 0.0)
        nanPath.lineTo(1.0, 0.0)
        nanPath.lineTo(1.0, 1.0)
        nanPath.closePath()

        try {
            booleanOperation(
                nanPath,
                validPath,
                BooleanOperation.UNION,
                FillRule.EVEN_ODD
            )
            // If we reach here, the operation didn't throw an exception as expected
            throw AssertionError("Expected LineSweeperException but operation succeeded")
        } catch (e: LineSweeperException) {
            // Expected for invalid input - could be NaNInput, InfiniteInput, or InternalException
            assertThat(e).isNotNull()
        }

        // Test with infinite coordinates - should throw some form of LineSweeperException
        val infinitePath = BezierPath()
        infinitePath.moveTo(Double.POSITIVE_INFINITY, 0.0)
        infinitePath.lineTo(1.0, 0.0)
        infinitePath.lineTo(1.0, 1.0)
        infinitePath.closePath()

        try {
            booleanOperation(
                infinitePath,
                validPath,
                BooleanOperation.UNION,
                FillRule.EVEN_ODD
            )
            // If we reach here, the operation didn't throw an exception as expected
            throw AssertionError("Expected InfiniteInput exception but operation succeeded")
        } catch (e: LineSweeperException.InfiniteInput) {
            // Expected for infinite input
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun testSegmentExtraction() {
        // Create a simple path with different segment types
        val path = BezierPath()
        path.moveTo(0.0, 0.0)
        path.lineTo(1.0, 0.0)
        path.quadTo(1.5, 0.5, 2.0, 0.0)
        path.curveTo(2.5, 0.0, 3.0, 0.5, 3.0, 1.0)
        path.closePath()

        val segments = path.getSegments()
        assertThat(segments).hasSize(5)

        // Check MoveTo segment
        val moveTo = segments[0]
        assertThat(moveTo).isInstanceOf(PathSegment.MoveTo::class.java)
        val moveToSegment = moveTo as PathSegment.MoveTo
        assertThat(moveToSegment.x).isEqualTo(0.0)
        assertThat(moveToSegment.y).isEqualTo(0.0)

        // Check LineTo segment
        val lineTo = segments[1]
        assertThat(lineTo).isInstanceOf(PathSegment.LineTo::class.java)
        val lineToSegment = lineTo as PathSegment.LineTo
        assertThat(lineToSegment.x).isEqualTo(1.0)
        assertThat(lineToSegment.y).isEqualTo(0.0)

        // Check QuadTo segment
        val quadTo = segments[2]
        assertThat(quadTo).isInstanceOf(PathSegment.QuadTo::class.java)
        val quadToSegment = quadTo as PathSegment.QuadTo
        assertThat(quadToSegment.cpX).isEqualTo(1.5)
        assertThat(quadToSegment.cpY).isEqualTo(0.5)
        assertThat(quadToSegment.x).isEqualTo(2.0)
        assertThat(quadToSegment.y).isEqualTo(0.0)

        // Check CurveTo segment
        val curveTo = segments[3]
        assertThat(curveTo).isInstanceOf(PathSegment.CurveTo::class.java)
        val curveToSegment = curveTo as PathSegment.CurveTo
        assertThat(curveToSegment.cp1X).isEqualTo(2.5)
        assertThat(curveToSegment.cp1Y).isEqualTo(0.0)
        assertThat(curveToSegment.cp2X).isEqualTo(3.0)
        assertThat(curveToSegment.cp2Y).isEqualTo(0.5)
        assertThat(curveToSegment.x).isEqualTo(3.0)
        assertThat(curveToSegment.y).isEqualTo(1.0)

        // Check ClosePath segment
        val closePath = segments[4]
        assertThat(closePath).isInstanceOf(PathSegment.ClosePath::class.java)
    }

    @Test
    fun testSegmentExtractionFromBooleanResult() {
        // Create two simple rectangles
        val rect1 = BezierPath()
        rect1.moveTo(0.0, 0.0)
        rect1.lineTo(2.0, 0.0)
        rect1.lineTo(2.0, 2.0)
        rect1.lineTo(0.0, 2.0)
        rect1.closePath()

        val rect2 = BezierPath()
        rect2.moveTo(1.0, 1.0)
        rect2.lineTo(3.0, 1.0)
        rect2.lineTo(3.0, 3.0)
        rect2.lineTo(1.0, 3.0)
        rect2.closePath()

        // Perform union operation
        val result = booleanOperation(
            rect1,
            rect2,
            BooleanOperation.UNION,
            FillRule.EVEN_ODD
        )

        assertThat(result.paths).isNotEmpty()

        // Extract segments from the result path
        for (path in result.paths) {
            val segments = path.getSegments()
            assertThat(segments).isNotEmpty()

            // First segment should be MoveTo
            assertThat(segments[0]).isInstanceOf(PathSegment.MoveTo::class.java)

            // Should have a ClosePath at the end
            assertThat(segments.last()).isInstanceOf(PathSegment.ClosePath::class.java)

            // Print segments for debugging
            for ((index, segment) in segments.withIndex()) {
                when (segment) {
                    is PathSegment.MoveTo -> println("Segment $index: MoveTo(${segment.x}, ${segment.y})")
                    is PathSegment.LineTo -> println("Segment $index: LineTo(${segment.x}, ${segment.y})")
                    is PathSegment.QuadTo -> println("Segment $index: QuadTo(cp: ${segment.cpX}, ${segment.cpY}, to: ${segment.x}, ${segment.y})")
                    is PathSegment.CurveTo -> println("Segment $index: CurveTo(cp1: ${segment.cp1X}, ${segment.cp1Y}, cp2: ${segment.cp2X}, ${segment.cp2Y}, to: ${segment.x}, ${segment.y})")
                    is PathSegment.ClosePath -> println("Segment $index: ClosePath")
                }
            }
        }
    }

    private fun createCircle(centerX: Double, centerY: Double, radius: Double): BezierPath {
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
}
