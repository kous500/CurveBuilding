package me.kous500.curvebuilding.commands;

import me.kous500.curvebuilding.GameMessage;

/** Reads the arguments of a command, so that an error is the error of the argument it is about. */
public final class CommandArguments {
    private CommandArguments() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public interface Parser<T> {
        T parse(String text) throws ArgumentException;
    }

    /** {@code args[index]} read by {@code parser}. A missing argument is an error too. */
    public static <T> T read(String[] args, int index, Parser<T> parser) throws ArgumentException {
        if (index >= args.length) {
            throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(index);
        }
        try {
            return parser.parse(args[index]);
        } catch (ArgumentException e) {
            throw e.at(index);
        }
    }
}
