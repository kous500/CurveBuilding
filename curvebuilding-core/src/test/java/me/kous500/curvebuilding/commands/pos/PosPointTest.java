package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.curve.CurvePoint.Part;
import me.kous500.curvebuilding.message.GameMessage;
import me.kous500.curvebuilding.message.Message;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosPointTest {
    private static final int MAX_POS_VALUE = 100;

    @ParameterizedTest
    @CsvSource({
            "3,    3,   POINT",
            "3f,   3,   F",
            "3b,   3,   B",
            "1b,   1,   B",
            "2f,   2,   F",
            "100,  100, POINT",
            "100b, 100, B",
    })
    void parsesValidPos(String input, int n, Part part) throws ArgumentException {
        assertEquals(new PosPoint(n, part), PosPoint.parse(input, MAX_POS_VALUE));
    }

    @ParameterizedTest
    @CsvSource({
            "1f,          INCORRECT_ARGUMENT",
            "abc,         INCORRECT_ARGUMENT",
            "3x,          INCORRECT_ARGUMENT",
            "3fx,         INCORRECT_ARGUMENT",
            "3bf,         INCORRECT_ARGUMENT",
            "-1,          INCORRECT_ARGUMENT",
            "f,           INCORRECT_ARGUMENT",
            "'',          INCORRECT_ARGUMENT",
            "0,           INTEGER_TOO_LOW",
            "0f,          INTEGER_TOO_LOW",
            "99999999999, INVALID_INTEGER",
    })
    void rejectsInvalidPos(String input, GameMessage expected) {
        ArgumentException error = assertThrows(ArgumentException.class, () -> PosPoint.parse(input, MAX_POS_VALUE));
        assertEquals(expected, error.message());
    }

    @ParameterizedTest
    @CsvSource({"101", "101f"})
    void rejectsPosAboveTheMaximum(String input) {
        ArgumentException error = assertThrows(ArgumentException.class, () -> PosPoint.parse(input, MAX_POS_VALUE));
        assertEquals(Message.MAX_POS_VALUE, error.message());
    }

    @ParameterizedTest
    @CsvSource({"2f, INCORRECT_ARGUMENT", "2b, INCORRECT_ARGUMENT", "0, INTEGER_TOO_LOW"})
    void numberHasNoHandle(String input, GameMessage expected) throws ArgumentException {
        assertEquals(2, PosPoint.parseNumber("2", MAX_POS_VALUE));
        ArgumentException error = assertThrows(
                ArgumentException.class,
                () -> PosPoint.parseNumber(input, MAX_POS_VALUE)
        );
        assertEquals(expected, error.message());
    }
}
