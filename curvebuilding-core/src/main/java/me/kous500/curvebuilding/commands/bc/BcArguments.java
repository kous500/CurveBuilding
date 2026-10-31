package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.commands.IntegerArgument;

import static me.kous500.curvebuilding.commands.CommandArguments.read;

/**
 * The arguments of {@code //bc}: options such as {@code -a} (see {@link BcOptions}) and an offset, in any order.
 *
 * @param offset how many blocks into the repeating selection the curve starts, counting back when negative
 * @param air whether only air blocks are replaced
 * @param axis the axis to read the selection along, or null to choose it from the selection
 */
public record BcArguments(int offset, boolean air, BcOptions.Axis axis) {
    /** Reads {@code args}, and stops at the first argument that cannot be used. */
    public static BcArguments parse(String[] args) throws ArgumentException {
        BcOptions options = new BcOptions();
        Integer offset = null;
        for (int i = 0; i < args.length; i++) {
            // A "-" before a letter starts options; before anything else, such as -5, it is a number
            if (args[i].matches("-([a-zA-Z].*)?")) {
                if (args[i].length() == 1 || !options.read(args[i].substring(1))) {
                    throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(i);
                }
            } else {
                if (offset != null) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(i);
                offset = read(args, i, text -> IntegerArgument.parse(text, Integer.MIN_VALUE));
            }
        }
        return new BcArguments(offset != null ? offset : 0, options.air(), options.axis());
    }
}
