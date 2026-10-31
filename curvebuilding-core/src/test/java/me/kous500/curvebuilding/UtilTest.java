package me.kous500.curvebuilding;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtilTest {
    @Nested
    class ChangeR {
        @Test
        void scalesToNewLength() {
            assertArrayEquals(new double[]{6, 8, 0}, Util.changeR(3, 4, 0, 10), 1e-9);
        }

        @Test
        void negativeLengthReversesDirection() {
            assertArrayEquals(new double[]{-1, -2, -2}, Util.changeR(1, 2, 2, -3), 1e-9);
        }

        @Test
        void zeroVectorStaysZero() {
            assertArrayEquals(new double[]{0, 0, 0}, Util.changeR(0, 0, 0, 5), 1e-9);
        }
    }

    @Nested
    class MessageReplace {
        @Test
        void replacesPlaceholdersInOrder() {
            assertEquals("5 blocks, 2.5 m", Util.messageReplace("{0} blocks, {1} m", 5, 2.5));
        }

        @Test
        void replacesEveryOccurrence() {
            assertEquals("a-a", Util.messageReplace("{0}-{0}", "a"));
        }

        @Test
        void keepsPlaceholdersWithoutArguments() {
            assertEquals("a {1}", Util.messageReplace("{0} {1}", "a"));
        }

        @Test
        void handlesNull() {
            assertNull(Util.messageReplace(null, "a"));
            assertEquals("{0}", Util.messageReplace("{0}", (Object[]) null));
        }
    }
}
