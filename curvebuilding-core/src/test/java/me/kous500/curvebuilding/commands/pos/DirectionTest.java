package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.commands.ArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

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

    @Test
    void firstLettersDiffer() {
        Set<Character> letters = new HashSet<>();
        for (Direction direction : Direction.values()) {
            assertTrue(letters.add(direction.label.charAt(0)), direction + " shares its first letter");
        }
    }
}
