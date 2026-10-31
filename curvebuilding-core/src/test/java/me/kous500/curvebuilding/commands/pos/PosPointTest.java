package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.CurveBuilding;
import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosPointTest {
    private Config previousConfig;

    @BeforeEach
    void setUpConfig() {
        previousConfig = CurveBuilding.config;
        Config config = new Config();
        config.maxPosValue = 100;
        CurveBuilding.config = config;
    }

    @AfterEach
    void restoreConfig() {
        CurveBuilding.config = previousConfig;
    }

    // h: 0 = connection point, 1 = control point f, 2 = control point b
    @ParameterizedTest
    @CsvSource({
            "3,    3,   0",
            "3f,   3,   1",
            "3b,   3,   2",
            "1b,   1,   2",
            "2f,   2,   1",
            "100,  100, 0",
            "100b, 100, 2",
    })
    void parsesValidPos(String input, int n, int h) throws ArgumentException {
        assertEquals(new PosPoint(n, h), PosPoint.parse(input));
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
        assertEquals(expected, assertThrows(ArgumentException.class, () -> PosPoint.parse(input)).message());
    }

    @ParameterizedTest
    @CsvSource({"101", "101f"})
    void rejectsPosAboveTheMaximum(String input) {
        assertEquals(Message.MAX_POS_VALUE, assertThrows(ArgumentException.class, () -> PosPoint.parse(input)).message());
    }

    @ParameterizedTest
    @CsvSource({"2f, INCORRECT_ARGUMENT", "2b, INCORRECT_ARGUMENT", "0, INTEGER_TOO_LOW"})
    void numberHasNoHandle(String input, GameMessage expected) throws ArgumentException {
        assertEquals(2, PosPoint.parseNumber("2"));
        assertEquals(expected, assertThrows(ArgumentException.class, () -> PosPoint.parseNumber(input)).message());
    }
}
