package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.message.GameMessage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosCoordinatesTest {
    /** The coordinates after {@code set 2}, as in {@code //bcpos set 2 x y z}. */
    private static PosCoordinates parse(String x, String y, String z) throws ArgumentException {
        return PosCoordinates.parse(new String[]{"set", "2", x, y, z}, 2);
    }

    @Nested
    class World {
        private static final Vector3 FEET = Vector3.at(10.7, 64.2, -3.4);

        @Test
        void numbersAreTheCoordinates() throws ArgumentException {
            assertVector(100, -5, -20, parse("100", "-5", "-20").position(FEET, 0, 0));
            assertVector(100.5, -5.5, -20.9, parse("100.5", "-5.5", "-20.9").position(FEET, 0, 0));
        }

        @Test
        void tildeAloneIsHalfABlockFromTheFeetOnXAndZ() throws ArgumentException {
            assertVector(10.2, 64.2, -3.9, parse("~", "~", "~").position(FEET, 0, 0));
            assertVector(10.7, 64.2, -3.4, parse("~0.5", "~", "~0.5").position(FEET, 0, 0));
        }

        @Test
        void tildeAloneInTheMiddleOfABlockIsItsCorner() throws ArgumentException {
            assertVector(10, 64, -4, parse("~", "~", "~").position(Vector3.at(10.5, 64, -3.5), 0, 0));
        }

        @Test
        void tildeAddsToThatPosition() throws ArgumentException {
            assertVector(11.7, 63.2, -3.9, parse("~1.5", "~-1", "~").position(FEET, 0, 0));
        }

        @Test
        void tildeAndNumbersMix() throws ArgumentException {
            assertVector(100, 64.2, -20, parse("100", "~", "-20").position(FEET, 90, -30));
        }
    }

    @Nested
    class Local {
        // In the middle of the block at the origin, so that the coordinates count from its corner
        private static final Vector3 FEET = Vector3.at(0.5, 64, 0.5);

        @Test
        void facingSouthLeftIsEastAndForwardIsSouth() throws ArgumentException {
            assertVector(2, 65, 3, parse("^2", "^1", "^3").position(FEET, 0, 0));
        }

        @Test
        void facingWestForwardIsWest() throws ArgumentException {
            assertVector(-3, 64, 0, parse("^", "^", "^3").position(FEET, 90, 0));
        }

        @Test
        void lookingStraightUpForwardIsUpAndUpIsBack() throws ArgumentException {
            assertVector(0, 67, 0, parse("^", "^", "^3").position(FEET, 0, -90));
            assertVector(0, 64, -2, parse("^", "^2", "^").position(FEET, 0, -90));
        }

        @Test
        void caretAloneIsLikeTildeAlone() throws ArgumentException {
            assertVector(0, 64, 0, parse("^", "^", "^").position(FEET, 37, 12));
        }
    }

    @ParameterizedTest
    @CsvSource({
            // The argument with the problem, counted from the x coordinate
            "^,    ~,    ^,    1, MIXED_COORDINATES",
            "1,    ^,    1,    1, MIXED_COORDINATES",
            "~,    ~,    ^2,   2, MIXED_COORDINATES",
            "1.2.3, 2,   3,    0, INVALID_DOUBLE",
            "1,    ~x,   3,    1, INVALID_DOUBLE",
            "^,    ^,    ^1.2.3, 2, INVALID_DOUBLE",
            "abc,  2,    3,    0, INVALID_DOUBLE",
    })
    void rejectsCoordinatesItCannotRead(String x, String y, String z, int wrong, GameMessage message) {
        ArgumentException e = assertThrows(ArgumentException.class, () -> parse(x, y, z));
        assertEquals(message, e.message());
        assertEquals(2 + wrong, e.index());
    }

    @Test
    void needsAllThreeCoordinates() {
        ArgumentException e = assertThrows(ArgumentException.class,
                () -> PosCoordinates.parse(new String[]{"set", "2", "1", "2"}, 2));
        assertEquals(GameMessage.INCOMPLETE_POSITION, e.message());
        // The missing one, after the last argument
        assertEquals(4, e.index());
    }
}
