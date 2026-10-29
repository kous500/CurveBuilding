package me.kous500.curvebuilding.commands.bc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BcCompletionTest {
    private static List<String> complete(String... args) {
        return BcCompletion.complete(args);
    }

    @Test
    void everyOptionAfterADash() {
        assertEquals(List.of("-a", "-x", "-z"), complete("-"));
    }

    @Test
    void optionsNotYetUsed() {
        assertEquals(List.of("-ax", "-az"), complete("-a"));
        // x and z are two ways to set one direction
        assertEquals(List.of("-xa"), complete("-x"));
        assertEquals(List.of(), complete("-ax"));
    }

    @Test
    void optionsUsedInEarlierArguments() {
        assertEquals(List.of("-x", "-z"), complete("-a", "-"));
        assertEquals(List.of("-a"), complete("-z", "-"));
    }

    @Test
    void nothingForOtherArguments() {
        assertEquals(List.of(), complete());
        assertEquals(List.of(), complete(""));
        assertEquals(List.of(), complete("5"));
        assertEquals(List.of(), complete("-5"));
        assertEquals(List.of(), complete("-aa"));
        assertEquals(List.of(), complete("-q"));
    }
}
