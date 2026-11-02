package me.kous500.curvebuilding.commands;

import me.kous500.curvebuilding.GameMessage;

/** Reads the arguments of a command, so that an error is the error of the argument it is about. */
public final class CommandArguments {
    private CommandArguments() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public interface Parser<T> {
        /**
         * Reads one argument.
         *
         * @param text the argument as typed
         * @return the value of the argument
         * @throws ArgumentException when the argument cannot be used
         */
        T parse(String text) throws ArgumentException;
    }

    /**
     * Reads {@code args[index]} with {@code parser}, so that an error is the error of that argument. A missing argument
     * is an error too.
     *
     * @param args the arguments of the command
     * @param index the index of the argument to read
     * @param parser reads the argument
     * @param <T> the type of the value
     * @return the value of the argument
     * @throws ArgumentException when the argument is missing or cannot be used, with {@code index} set
     */
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
