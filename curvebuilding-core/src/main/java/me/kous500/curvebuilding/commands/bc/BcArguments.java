package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.commands.IntegerArgument;

import static me.kous500.curvebuilding.commands.CommandArguments.read;

/**
 * The arguments of {@code //bc}: options such as {@code -a} (see {@link BcOptions}) and up to two numbers, in any order.
 *
 * @param n a plane is set every {@code n} blocks along the curve, or 0
 * @param m the first plane is set after {@code m} blocks, or 0
 * @param air whether only air blocks are replaced
 * @param axis the axis to read the selection along, or null to choose it from the selection
 */
public record BcArguments(int n, int m, boolean air, BcOptions.Axis axis) {
    /** Reads {@code args}, and stops at the first argument that cannot be used. */
    public static BcArguments parse(String[] args) throws ArgumentException {
        BcOptions options = new BcOptions();
        int[] numbers = new int[2];
        int count = 0;
        for (int i = 0; i < args.length; i++) {
            // A "-" before a letter starts options; before anything else, such as -5, it is a number
            if (args[i].matches("-([a-zA-Z].*)?")) {
                if (args[i].length() == 1 || !options.read(args[i].substring(1))) {
                    throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(i);
                }
            } else {
                if (count == numbers.length) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(i);
                numbers[count++] = read(args, i, text -> IntegerArgument.parse(text, 0));
            }
        }
        return new BcArguments(numbers[0], numbers[1], options.air(), options.axis());
    }
}
