package me.kous500.curvebuilding;

import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        void curveLengthConvergesWithFineness() {
            assertEquals(Util.bezierLength(ARCH, 100_000), Util.bezierLength(ARCH, 1000), 1e-4);
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
