package me.kous500.curvebuilding.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {
    @Test
    void replacesPlaceholdersInOrder() {
        assertEquals("5 blocks, 2.5 m", Messages.replacePlaceholders("{0} blocks, {1} m", 5, 2.5));
    }

    @Test
    void replacesEveryOccurrence() {
        assertEquals("a-a", Messages.replacePlaceholders("{0}-{0}", "a"));
    }

    @Test
    void keepsPlaceholdersWithoutArguments() {
        assertEquals("a {1}", Messages.replacePlaceholders("{0} {1}", "a"));
    }
}
