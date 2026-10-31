package me.kous500.curvebuilding.commands;

import me.kous500.curvebuilding.GameMessage;

/** Reads an integer argument, with the errors of the game's own integer arguments. */
public final class IntegerArgument {
    private IntegerArgument() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static int parse(String text, int min) throws ArgumentException {
        if (!text.matches("-?[0-9]+")) {
            // Something that looks like a number, such as 2.5, is an invalid integer rather than a wrong argument
            throw text.matches("[+-]?[0-9]*(\\.[0-9]+)?")
                    ? new ArgumentException(GameMessage.INVALID_INTEGER, text)
                    : new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ArgumentException(GameMessage.INVALID_INTEGER, text);
        }
        if (value < min) {
            throw new ArgumentException(GameMessage.INTEGER_TOO_LOW, min, text);
        }
        return value;
    }
}
