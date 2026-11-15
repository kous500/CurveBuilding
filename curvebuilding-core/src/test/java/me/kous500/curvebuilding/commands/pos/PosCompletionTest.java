package me.kous500.curvebuilding.commands.pos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosCompletionTest {
    private static List<String> complete(String... args) {
        return PosCompletion.complete(args);
    }

    @Test
    void subcommands() {
        assertEquals(List.of("clear", "clearall", "insert", "remove", "set", "shift"), complete(""));
        assertEquals(List.of("clear", "clearall"), complete("cl"));
        assertEquals(List.of("set", "shift"), complete("S"));
    }

    @Test
    void positionsForAnEmptyArgument() {
        List<String> oneToNine = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
        for (String subcommand : new String[]{"set", "clear", "insert", "remove"}) {
            assertEquals(oneToNine, complete(subcommand, ""), subcommand);
        }
    }

    @Test
    void handlesAfterAPositionNumber() {
        assertEquals(List.of("3b", "3f"), complete("set", "3"));
        // A complete position gets nothing more
        assertEquals(List.of(), complete("clear", "3b"));
        // The first position has no front handle
        assertEquals(List.of("1b"), complete("set", "1"));
    }

    @Test
    void tildesUpToTheLastCoordinate() {
        for (String subcommand : new String[]{"set", "insert"}) {
            assertEquals(List.of("~", "~ ~", "~ ~ ~"), complete(subcommand, "2", ""), subcommand);
        }
        assertEquals(List.of("~", "~ ~", "~ ~ ~"), complete("set", "2", "~"));
        assertEquals(List.of("~", "~ ~"), complete("set", "2", "~", ""));
        assertEquals(List.of("~", "~ ~"), complete("set", "2", "100", ""));
        assertEquals(List.of("~"), complete("set", "2", "~", "~", ""));
        assertEquals(List.of("~"), complete("set", "2", "100", "64", ""));
    }

    @Test
    void numberWithTildesAfterIt() {
        assertEquals(List.of("100 ~", "100 ~ ~"), complete("set", "2", "100"));
        assertEquals(List.of("~5 ~", "~5 ~ ~"), complete("set", "2", "~5"));
        assertEquals(List.of("64 ~"), complete("set", "2", "100", "64"));
        // Nothing after the last coordinate, nor after something that is not a number
        assertEquals(List.of(), complete("set", "2", "100", "64", "-20"));
        assertEquals(List.of(), complete("set", "2", "abc"));
    }

    @Test
    void caretOnlyAfterACaretFirst() {
        assertEquals(List.of("^ ^ ^"), complete("set", "2", "^"));
        assertEquals(List.of("^5 ^ ^"), complete("set", "2", "^5"));
        assertEquals(List.of("^ ^"), complete("set", "2", "^", ""));
        assertEquals(List.of("^3 ^"), complete("set", "2", "^", "^3"));
        assertEquals(List.of("^"), complete("set", "2", "^", "^", ""));
        // A caret cannot follow other coordinates, nor others a caret
        assertEquals(List.of(), complete("set", "2", "100", "^"));
        assertEquals(List.of(), complete("set", "2", "^", "5"));
    }

    @Test
    void noCoordinatesAfterTheLastOneOrForOtherSubcommands() {
        assertEquals(List.of(), complete("set", "2", "~", "~", "~", ""));
        assertEquals(List.of(), complete("clear", "2", ""));
        assertEquals(List.of(), complete("remove", "2", ""));
    }

    @Test
    void moreDigitsAfterAPointNumber() {
        assertEquals(List.of("20", "21", "22", "23", "24", "25", "26", "27", "28", "29"), complete("insert", "2"));
    }

    @Test
    void lengthWithEachDirectionForShift() {
        List<String> expected = List.of(
                "5 back",
                "5 down",
                "5 east",
                "5 forward",
                "5 left",
                "5 me",
                "5 north",
                "5 right",
                "5 south",
                "5 up",
                "5 west"
        );
        assertEquals(expected, complete("shift", "5"));
        assertEquals(List.of(), complete("shift", ""));
        assertEquals(List.of(), complete("shift", "x"));
    }

    @Test
    void nothingElse() {
        assertEquals(List.of(), complete());
        assertEquals(List.of(), complete("clearall", ""));
        assertEquals(List.of(), complete("set", "0"));
        assertEquals(List.of(), complete("set", "x"));
        assertEquals(List.of(), complete("unknown", ""));
        assertEquals(List.of(), complete("shift", "5", ""));
    }
}
