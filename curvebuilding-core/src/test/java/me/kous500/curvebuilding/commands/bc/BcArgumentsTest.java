package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BcArgumentsTest {
    private static BcArguments parse(String args) throws ArgumentException {
        return BcArguments.parse(args.isEmpty() ? new String[0] : args.split(" "));
    }

    @Test
    void readsOptionsAndNumbersInAnyOrder() throws ArgumentException {
        assertEquals(new BcArguments(0, 0, false, null), parse(""));
        assertEquals(new BcArguments(5, 0, false, null), parse("5"));
        assertEquals(new BcArguments(5, 2, true, BcOptions.Axis.X), parse("-ax 5 2"));
        assertEquals(new BcArguments(5, 2, true, BcOptions.Axis.Z), parse("5 -z 2 -a"));
    }

    // index: the argument the error points at
    @ParameterizedTest
    @CsvSource({
            "abc def,  INCORRECT_ARGUMENT, 0",
            "5 abc,    INCORRECT_ARGUMENT, 1",
            "1 2 3,    INCORRECT_ARGUMENT, 2",
            "-5,       INTEGER_TOO_LOW,    0",
            "-2.5,     INVALID_INTEGER,    0",
            "2.5,      INVALID_INTEGER,    0",
            "-,        INCORRECT_ARGUMENT, 0",
            "-aa,      INCORRECT_ARGUMENT, 0",
            "-xz,      INCORRECT_ARGUMENT, 0",
            "-q,       INCORRECT_ARGUMENT, 0",
            "-A,       INCORRECT_ARGUMENT, 0",
            "-x 5 -z,  INCORRECT_ARGUMENT, 2",
    })
    void stopsAtTheFirstArgumentThatCannotBeUsed(String args, GameMessage expected, int index) {
        ArgumentException e = assertThrows(ArgumentException.class, () -> parse(args));
        assertEquals(expected, e.message());
        assertEquals(index, e.index());
    }
}
