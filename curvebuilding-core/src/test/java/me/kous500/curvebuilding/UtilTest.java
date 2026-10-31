package me.kous500.curvebuilding;

import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilTest {
    private static final Vector3[] ARCH = {
            Vector3.at(0, 0, 0),
            Vector3.at(0, 10, 0),
            Vector3.at(10, 10, 0),
            Vector3.at(10, 0, 0)
    };

    @Nested
    class BezierCoordinate {
        @Test
        void endpointsAreShiftedToBlockCenter() {
            assertVector(0.5, 0.5, 0.5, Util.bezierCoordinate(ARCH, 0));
            assertVector(10.5, 0.5, 0.5, Util.bezierCoordinate(ARCH, 1));
        }

        @Test
        void midpointUsesCubicBernsteinWeights() {
            // weights at t = 0.5 are 1/8, 3/8, 3/8, 1/8
            assertVector(5.5, 8.0, 0.5, Util.bezierCoordinate(ARCH, 0.5));
        }

        @Test
        void quarterPoint() {
            // weights at t = 0.25 are 27/64, 27/64, 9/64, 1/64
            assertVector(10.0 * 10 / 64 + 0.5, 10.0 * 36 / 64 + 0.5, 0.5, Util.bezierCoordinate(ARCH, 0.25));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.001, 1.001})
        void rejectsPositionOutsideCurve(double t) {
            assertThrows(IllegalArgumentException.class, () -> Util.bezierCoordinate(ARCH, t));
        }

        @Test
        void rejectsIncompletePositions() {
            assertThrows(NullPointerException.class, () -> Util.bezierCoordinate(null, 0.5));
            assertThrows(IllegalArgumentException.class,
                    () -> Util.bezierCoordinate(new Vector3[]{ARCH[0], ARCH[1], ARCH[2]}, 0.5));
            assertThrows(IllegalArgumentException.class,
                    () -> Util.bezierCoordinate(new Vector3[]{ARCH[0], null, ARCH[2], ARCH[3]}, 0.5));
        }
    }

    @Nested
    class BezierLength {
        private static Vector3[] straight(Vector3 from, Vector3 to) {
            return new Vector3[]{from, from, to, to};
        }

        @ParameterizedTest
        @ValueSource(doubles = {1, 10, 100, 1000})
        void straightLineHasItsDistance(double fineness) {
            assertEquals(10, Util.bezierLength(straight(Vector3.at(0, 64, 0), Vector3.at(10, 64, 0)), fineness), 1e-9);
        }

        @Test
        void diagonalLineHasItsDistance() {
            assertEquals(15, Util.bezierLength(straight(Vector3.at(-3, 70, 2), Vector3.at(2, 80, 12)), 100), 1e-9);
        }

        @Test
        void parabolaHasItsArcLength() {
            // x = 10t, y = 10t^2, whose arc length is 10 * (sqrt(5) / 2 + asinh(2) / 4)
            Vector3[] parabola = {
                    Vector3.at(0, 0, 0),
                    Vector3.at(10.0 / 3, 0, 0),
                    Vector3.at(20.0 / 3, 10.0 / 3, 0),
                    Vector3.at(10, 10, 0)
            };
            double expected = 10 * (Math.sqrt(5) / 2 + Math.log(2 + Math.sqrt(5)) / 4);

            assertEquals(expected, Util.bezierLength(parabola, 100), 1e-3);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1})
        void rejectsNonPositiveFineness(double fineness) {
            assertThrows(IllegalArgumentException.class, () -> Util.bezierLength(ARCH, fineness));
        }

        @Test
        void rejectsIncompletePositions() {
            assertThrows(NullPointerException.class, () -> Util.bezierLength(null, 10));
            assertThrows(IllegalArgumentException.class,
                    () -> Util.bezierLength(new Vector3[]{ARCH[0], ARCH[1], null, ARCH[3]}, 10));
        }
    }

    @Nested
    class BezierFineness {
        private static final Vector3 A = Vector3.at(0, 64, 0);

        @Test
        void segmentWithoutHandlesUsesTheDistanceBetweenItsEnds() {
            Vector3 b = Vector3.at(10, 64, 0);
            assertEquals(200, Util.bezierFineness(new Vector3[]{A, A, b, b}, 20), 1e-9);
        }

        @Test
        void loopWithItsEndsAtTheSamePlaceGetsStepsAlongItsHandles() {
            Vector3[] loop = {A, Vector3.at(0, 64, 10), Vector3.at(10, 64, 10), A};
            double fineness = Util.bezierFineness(loop, 20);

            assertEquals((10 + 10 + Math.sqrt(200)) * 20, fineness, 1e-9);
            double exact = Util.bezierLength(loop, 100_000);
            assertEquals(exact, Util.bezierLength(loop, fineness), exact * 0.01);
        }

        @Test
        void segmentWithEveryPointAtTheSamePlaceGetsOneStep() {
            Vector3[] point = {A, A, A, A};
            assertEquals(1, Util.bezierFineness(point, 20));
            assertEquals(0, Util.bezierLength(point, Util.bezierFineness(point, 20)));
        }

        @Test
        void rejectsIncompletePositions() {
            assertThrows(NullPointerException.class, () -> Util.bezierFineness(null, 20));
            assertThrows(IllegalArgumentException.class,
                    () -> Util.bezierFineness(new Vector3[]{A, A, null, A}, 20));
        }
    }

    @Nested
    class BezierSegments {
        private final Vector3 a = Vector3.at(0, 64, 0);
        private final Vector3 aBack = Vector3.at(0, 64, 3);
        private final Vector3 b = Vector3.at(10, 64, 0);
        private final Vector3 bFront = Vector3.at(10, 64, -3);
        private final Vector3 c = Vector3.at(20, 64, 0);

        @Test
        void connectsConsecutivePositionsThroughTheirHandles() {
            NavigableMap<Integer, CurvePoint> posMap = new TreeMap<>();
            posMap.put(1, new CurvePoint(a, null, aBack));
            posMap.put(2, new CurvePoint(b, bFront, null));

            List<Vector3[]> segments = Util.bezierSegments(posMap);

            assertEquals(1, segments.size());
            assertArrayEquals(new Vector3[]{a, aBack, bFront, b}, segments.get(0));
        }

        @Test
        void missingHandlesFallBackToTheirPositions() {
            NavigableMap<Integer, CurvePoint> posMap = new TreeMap<>();
            posMap.put(1, new CurvePoint(a, null, null));
            posMap.put(2, new CurvePoint(b, null, null));
            posMap.put(3, new CurvePoint(c, null, null));

            List<Vector3[]> segments = Util.bezierSegments(posMap);

            assertEquals(2, segments.size());
            assertArrayEquals(new Vector3[]{a, a, b, b}, segments.get(0));
            assertArrayEquals(new Vector3[]{b, b, c, c}, segments.get(1));
        }

        @Test
        void stopsAtTheFirstMissingPosition() {
            NavigableMap<Integer, CurvePoint> posMap = new TreeMap<>();
            posMap.put(1, new CurvePoint(a, null, null));
            posMap.put(2, new CurvePoint(b, null, null));
            posMap.put(4, new CurvePoint(c, null, null));
            posMap.put(5, new CurvePoint(a, null, null));

            assertEquals(1, Util.bezierSegments(posMap).size());

            posMap.put(3, new CurvePoint(null, bFront, null));
            assertEquals(1, Util.bezierSegments(posMap).size());
        }

        @Test
        void hasNoSegmentsWithoutTwoPositions() {
            NavigableMap<Integer, CurvePoint> posMap = new TreeMap<>();
            assertTrue(Util.bezierSegments(null).isEmpty());
            assertTrue(Util.bezierSegments(posMap).isEmpty());

            posMap.put(1, new CurvePoint(a, null, null));
            assertTrue(Util.bezierSegments(posMap).isEmpty());

            posMap.remove(1);
            posMap.put(2, new CurvePoint(b, null, null));
            assertTrue(Util.bezierSegments(posMap).isEmpty());
        }
    }

    @Nested
    class IsBezierSegmentWithin {
        @Test
        void checksTheEndsAndEachHandle() {
            assertTrue(Util.isBezierSegmentWithin(ARCH, 10));
            assertFalse(Util.isBezierSegmentWithin(ARCH, 9.9));

            Vector3[] farHandle = {Vector3.at(0, 0, 0), Vector3.at(0, 20, 0), Vector3.at(5, 0, 0), Vector3.at(5, 0, 0)};
            assertFalse(Util.isBezierSegmentWithin(farHandle, 10));

            Vector3[] farEndHandle = {Vector3.at(0, 0, 0), Vector3.at(0, 0, 0), Vector3.at(5, 20, 0), Vector3.at(5, 0, 0)};
            assertFalse(Util.isBezierSegmentWithin(farEndHandle, 10));
        }
    }

    @Nested
    class BezierPoints {
        @Test
        void includesBothEndsAndEvenSteps() {
            List<Vector3> points = Util.bezierPoints(ARCH, 4);

            assertEquals(5, points.size());
            assertVector(0.5, 0.5, 0.5, points.get(0));
            assertVector(5.5, 8.0, 0.5, points.get(2));
            assertVector(10.5, 0.5, 0.5, points.get(4));
        }

        @Test
        void oneDivisionGivesTheEnds() {
            List<Vector3> points = Util.bezierPoints(ARCH, 1);

            assertEquals(2, points.size());
            assertVector(0.5, 0.5, 0.5, points.get(0));
            assertVector(10.5, 0.5, 0.5, points.get(1));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void rejectsFewerThanOneDivision(int divisions) {
            assertThrows(IllegalArgumentException.class, () -> Util.bezierPoints(ARCH, divisions));
        }
    }

    @Nested
    class ChangeR {
        @Test
        void scalesToNewLength() {
            assertArrayEquals(new double[]{6, 8, 0}, Util.changeR(3, 4, 0, 10), 1e-9);
        }

        @Test
        void negativeLengthReversesDirection() {
            assertArrayEquals(new double[]{-1, -2, -2}, Util.changeR(1, 2, 2, -3), 1e-9);
        }

        @Test
        void zeroVectorStaysZero() {
            assertArrayEquals(new double[]{0, 0, 0}, Util.changeR(0, 0, 0, 5), 1e-9);
        }
    }

    @Nested
    class MessageReplace {
        @Test
        void replacesPlaceholdersInOrder() {
            assertEquals("5 blocks, 2.5 m", Util.messageReplace("{0} blocks, {1} m", 5, 2.5));
        }

        @Test
        void replacesEveryOccurrence() {
            assertEquals("a-a", Util.messageReplace("{0}-{0}", "a"));
        }

        @Test
        void keepsPlaceholdersWithoutArguments() {
            assertEquals("a {1}", Util.messageReplace("{0} {1}", "a"));
        }

        @Test
        void handlesNull() {
            assertNull(Util.messageReplace(null, "a"));
            assertEquals("{0}", Util.messageReplace("{0}", (Object[]) null));
        }
    }
}
