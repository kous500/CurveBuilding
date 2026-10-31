package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;

import java.util.Locale;

/** The directions of {@code //bcpos shift}. Their first letters differ, so each can also be typed as its first letter. */
public enum Direction {
    BACK, DOWN, EAST, FORWARD, LEFT, ME, NORTH, RIGHT, SOUTH, UP, WEST;

    /** The name to type, such as {@code back}. */
    public final String label = name().toLowerCase(Locale.ROOT);

    /** The direction typed as its name or its first letter, ignoring case. */
    public static Direction parse(String text) throws ArgumentException {
        String typed = text.toLowerCase(Locale.ROOT);
        for (Direction direction : values()) {
            if (direction.label.equals(typed) || direction.label.substring(0, 1).equals(typed)) return direction;
        }
        throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
    }
}
