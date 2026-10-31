package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.commands.ArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionTest {
    @ParameterizedTest
    @CsvSource({"back, BACK", "b, BACK", "West, WEST", "W, WEST", "me, ME", "forward, FORWARD"})
    void parsesTheNameOrTheFirstLetter(String input, Direction expected) throws ArgumentException {
        assertEquals(expected, Direction.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"banana", "ba", "x", ""})
    void rejectsOtherWords(String input) {
        assertThrows(ArgumentException.class, () -> Direction.parse(input));
    }

    // Minecraft's yaw: 0 looks south (+z), 90 west (-x), -90 east (+x), 180 north (-z); a positive pitch looks down
    @ParameterizedTest
    @CsvSource({
            "FORWARD, 0,    0,   0,  0,  5",
            "FORWARD, 90,   0,   -5, 0,  0",
            "FORWARD, 45,   0,   -5, 0,  5",
            "BACK,    0,    0,   0,  0,  -5",
            "RIGHT,   0,    0,   -5, 0,  0",
            "LEFT,    0,    0,   5,  0,  0",
            "ME,      -90,  0,   5,  0,  0",
            "ME,      0,    80,  0,  -5, 0",
            "ME,      0,    -80, 0,  5,  0",
            "FORWARD, 0,    80,  0,  0,  5",
            "UP,      0,    0,   0,  5,  0",
            "DOWN,    0,    0,   0,  -5, 0",
            "NORTH,   0,    0,   0,  0,  -5",
            "SOUTH,   180,  0,   0,  0,  5",
            "EAST,    0,    0,   5,  0,  0",
            "WEST,    0,    0,   -5, 0,  0",
            "BACK,    170,  0,   0,  0,  5",
    })
    void offsetFollowsWhereThePlayerLooks(Direction direction, double yaw, double pitch, double x, double y, double z) {
        assertVector(x, y, z, direction.offset(5, yaw, pitch));
    }

    @Test
    void firstLettersDiffer() {
        Set<Character> letters = new HashSet<>();
        for (Direction direction : Direction.values()) {
            assertTrue(letters.add(direction.label.charAt(0)), direction + " shares its first letter");
        }
    }
}
