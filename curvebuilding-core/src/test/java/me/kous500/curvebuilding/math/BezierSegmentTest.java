package me.kous500.curvebuilding.math;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BezierSegmentTest {
    private static final BezierSegment ARCH = new BezierSegment(
            Vector3.at(0, 0, 0),
            Vector3.at(0, 10, 0),
            Vector3.at(10, 10, 0),
            Vector3.at(10, 0, 0)
    );

    private static BezierSegment straight(Vector3 from, Vector3 to) {
        return new BezierSegment(from, from, to, to);
    }

    @Test
    void rejectsMissingPositions() {
        Vector3 a = Vector3.at(0, 0, 0);
        assertThrows(NullPointerException.class, () -> new BezierSegment(a, null, a, a));
    }

    @Nested
    class Point {
        @Test
        void endsAreTheEndPositions() {
            assertVector(0, 0, 0, ARCH.point(0));
            assertVector(10, 0, 0, ARCH.point(1));
        }

        @Test
        void midpointUsesCubicBernsteinWeights() {
            // weights at t = 0.5 are 1/8, 3/8, 3/8, 1/8
            assertVector(5, 7.5, 0, ARCH.point(0.5));
        }

        @Test
        void quarterPoint() {
            // weights at t = 0.25 are 27/64, 27/64, 9/64, 1/64
            assertVector(10.0 * 10 / 64, 10.0 * 36 / 64, 0, ARCH.point(0.25));
        }

        @Test
        void blockCenterIsHalfABlockFurther() {
            assertVector(0.5, 0.5, 0.5, ARCH.blockCenter(0));
            assertVector(5.5, 8.0, 0.5, ARCH.blockCenter(0.5));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.001, 1.001})
        void rejectsPositionOutsideCurve(double t) {
            assertThrows(IllegalArgumentException.class, () -> ARCH.point(t));
        }
    }

    @Nested
    class Angle {
        // dx and dz of the direction; the angle goes from +z towards +x
        @ParameterizedTest
        @CsvSource({
                "0,  1,  0",
                "1,  0,  1.5707963267948966",
                "0,  -1, 3.141592653589793",
                "-1, 0,  -1.5707963267948966",
                "1,  1,  0.7853981633974483",
                "0,  0,  0",
        })
        void followsTheCurveOnTheXzPlane(double dx, double dz, double expected) {
            BezierSegment segment = straight(Vector3.at(0, 64, 0), Vector3.at(dx * 10, 70, dz * 10));
            // The middle, as a straight segment without handles has no direction at its ends
            assertEquals(expected, segment.angle(0.5), 1e-12);
        }
    }

    @Nested
    class Length {
        @ParameterizedTest
        @ValueSource(doubles = {1, 10, 100, 1000})
        void straightLineHasItsDistance(double fineness) {
            assertEquals(10, straight(Vector3.at(0, 64, 0), Vector3.at(10, 64, 0)).length(fineness), 1e-9);
        }

        @Test
        void diagonalLineHasItsDistance() {
            assertEquals(15, straight(Vector3.at(-3, 70, 2), Vector3.at(2, 80, 12)).length(100), 1e-9);
        }

        @Test
        void parabolaHasItsArcLength() {
            // x = 10t, y = 10t^2, whose arc length is 10 * (sqrt(5) / 2 + asinh(2) / 4)
            BezierSegment parabola = new BezierSegment(
                    Vector3.at(0, 0, 0),
                    Vector3.at(10.0 / 3, 0, 0),
                    Vector3.at(20.0 / 3, 10.0 / 3, 0),
                    Vector3.at(10, 10, 0)
            );
            double expected = 10 * (Math.sqrt(5) / 2 + Math.log(2 + Math.sqrt(5)) / 4);

            assertEquals(expected, parabola.length(100), 1e-3);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1})
        void rejectsNonPositiveFineness(double fineness) {
            assertThrows(IllegalArgumentException.class, () -> ARCH.length(fineness));
        }
    }

    @Nested
    class Fineness {
        private static final Vector3 A = Vector3.at(0, 64, 0);

        @Test
        void segmentWithoutHandlesUsesTheDistanceBetweenItsEnds() {
            assertEquals(200, straight(A, Vector3.at(10, 64, 0)).fineness(20), 1e-9);
        }

        @Test
        void loopWithItsEndsAtTheSamePlaceGetsStepsAlongItsHandles() {
            BezierSegment loop = new BezierSegment(A, Vector3.at(0, 64, 10), Vector3.at(10, 64, 10), A);
            double fineness = loop.fineness(20);

            assertEquals((10 + 10 + Math.sqrt(200)) * 20, fineness, 1e-9);
            double exact = loop.length(100_000);
            assertEquals(exact, loop.length(fineness), exact * 0.01);
        }

        @Test
        void segmentWithEveryPointAtTheSamePlaceGetsOneStep() {
            BezierSegment point = new BezierSegment(A, A, A, A);
            assertEquals(1, point.fineness(20));
            assertEquals(0, point.length(point.fineness(20)));
        }
    }

    @Nested
    class Through {
        private final Vector3 a = Vector3.at(0, 64, 0);
        private final Vector3 aBack = Vector3.at(0, 64, 3);
        private final Vector3 b = Vector3.at(10, 64, 0);
        private final Vector3 bFront = Vector3.at(10, 64, -3);
        private final Vector3 c = Vector3.at(20, 64, 0);

        @Test
        void connectsConsecutivePositionsThroughTheirHandles() {
            NavigableMap<Integer, CurvePoint> points = new TreeMap<>();
            points.put(1, new CurvePoint(a, null, aBack));
            points.put(2, new CurvePoint(b, bFront, null));

            assertEquals(List.of(new BezierSegment(a, aBack, bFront, b)), BezierSegment.through(points));
        }

        @Test
        void missingHandlesFallBackToTheirPositions() {
            NavigableMap<Integer, CurvePoint> points = new TreeMap<>();
            points.put(1, new CurvePoint(a, null, null));
            points.put(2, new CurvePoint(b, null, null));
            points.put(3, new CurvePoint(c, null, null));

            assertEquals(List.of(straight(a, b), straight(b, c)), BezierSegment.through(points));
        }

        @Test
        void stopsAtTheFirstMissingPosition() {
            NavigableMap<Integer, CurvePoint> points = new TreeMap<>();
            points.put(1, new CurvePoint(a, null, null));
            points.put(2, new CurvePoint(b, null, null));
            points.put(4, new CurvePoint(c, null, null));
            points.put(5, new CurvePoint(a, null, null));

            assertEquals(1, BezierSegment.through(points).size());

            points.put(3, new CurvePoint(null, bFront, null));
            assertEquals(1, BezierSegment.through(points).size());
        }

        @Test
        void hasNoSegmentsWithoutTwoPositions() {
            NavigableMap<Integer, CurvePoint> points = new TreeMap<>();
            assertTrue(BezierSegment.through(null).isEmpty());
            assertTrue(BezierSegment.through(points).isEmpty());

            points.put(1, new CurvePoint(a, null, null));
            assertTrue(BezierSegment.through(points).isEmpty());

            points.remove(1);
            points.put(2, new CurvePoint(b, null, null));
            assertTrue(BezierSegment.through(points).isEmpty());
        }
    }

    @Test
    void isWithinChecksTheEndsAndEachHandle() {
        assertTrue(ARCH.isWithin(10));
        assertFalse(ARCH.isWithin(9.9));

        Vector3 origin = Vector3.at(0, 0, 0);
        Vector3 end = Vector3.at(5, 0, 0);
        assertFalse(new BezierSegment(origin, Vector3.at(0, 20, 0), end, end).isWithin(10));
        assertFalse(new BezierSegment(origin, origin, Vector3.at(5, 20, 0), end).isWithin(10));
    }

    @Nested
    class BlockCenters {
        @Test
        void includesBothEndsAndEvenSteps() {
            List<Vector3> points = ARCH.blockCenters(4);

            assertEquals(5, points.size());
            assertVector(0.5, 0.5, 0.5, points.get(0));
            assertVector(5.5, 8.0, 0.5, points.get(2));
            assertVector(10.5, 0.5, 0.5, points.get(4));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void rejectsFewerThanOneDivision(int divisions) {
            assertThrows(IllegalArgumentException.class, () -> ARCH.blockCenters(divisions));
        }
    }
}
