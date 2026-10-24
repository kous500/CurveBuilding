package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.CurveBuilding;
import me.kous500.curvebuilding.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PosTypeTest {
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
    void parsesValidPos(String input, int n, int h) {
        PosType posType = PosType.get(input);
        assertAll(
                () -> assertNull(posType.errorType),
                () -> assertEquals(n, posType.n),
                () -> assertEquals(h, posType.h)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "1f,          incorrectArgument",
            "abc,         incorrectArgument",
            "3x,          incorrectArgument",
            "-1,          incorrectArgument",
            "0,           integerLess",
            "0f,          integerLess",
            "101,         maxPosValue",
            "101f,        maxPosValue",
            "f,           invalidInteger",
            "'',          incorrectArgument",
            "99999999999, invalidInteger",
    })
    void rejectsInvalidPos(String input, PosType.ErrorType expected) {
        assertEquals(expected, PosType.get(input).errorType);
    }
}
