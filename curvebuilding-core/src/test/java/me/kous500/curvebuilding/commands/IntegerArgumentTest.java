package me.kous500.curvebuilding.commands;

import me.kous500.curvebuilding.GameMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegerArgumentTest {
    @Test
    void parsesIntegersFromTheMinimum() throws ArgumentException {
        assertEquals(0, IntegerArgument.parse("0", 0));
        assertEquals(12, IntegerArgument.parse("12", 0));
    }

    @ParameterizedTest
    @CsvSource({
            "abc,         INCORRECT_ARGUMENT",
            "5x,          INCORRECT_ARGUMENT",
            "2.5,         INVALID_INTEGER",
            "+5,          INVALID_INTEGER",
            "99999999999, INVALID_INTEGER",
            "-1,          INTEGER_TOO_LOW",
    })
    void rejectsOtherArguments(String input, GameMessage expected) {
        assertEquals(expected, assertThrows(ArgumentException.class, () -> IntegerArgument.parse(input, 0)).message());
    }

    @Test
    void tooLowGivesTheMinimumAndTheValue() {
        ArgumentException e = assertThrows(ArgumentException.class, () -> IntegerArgument.parse("-3", 0));
        assertEquals(List.of(0, "-3"), e.args());
    }
}
