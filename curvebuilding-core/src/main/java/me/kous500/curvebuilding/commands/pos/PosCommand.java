package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.commands.IntegerArgument;

import static me.kous500.curvebuilding.math.PosManager.*;

public final class PosCommand {
    private PosCommand() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Runs {@code //bcpos} with {@code args}, or shows the error of the first argument that cannot be used.
     *
     * @return whether the arguments could be used
     */
    public static boolean posCommand(Player player, String[] args) {
        // The argument being read, for the error
        int index = 0;
        try {
            PosSubcommand subcommand = PosSubcommand.of(argument(args, 0));
            if (subcommand == null) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);

            index = 1 + subcommand.arguments.max;
            if (args.length > index) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);

            index = 1;
            switch (subcommand) {
                case CLEARALL -> clearPos(player);
                case CLEAR -> {
                    PosPoint point = PosPoint.parse(argument(args, 1));
                    clearPos(player, point.n(), point.h());
                }
                case SET -> {
                    PosPoint point = PosPoint.parse(argument(args, 1));
                    addPos(player, point.n(), point.h());
                }
                case INSERT -> insert(player, PosPoint.parseNumber(argument(args, 1)));
                case REMOVE -> remove(player, PosPoint.parseNumber(argument(args, 1)));
                case SHIFT -> {
                    int length = IntegerArgument.parse(argument(args, 1), 0);
                    index = 2;
                    Direction direction = args.length > 2 ? Direction.parse(args[2]) : Direction.ME;
                    shift(player, length, direction);
                }
            }
            return true;
        } catch (ArgumentException e) {
            e.show(player, "bcpos", args, index);
            return false;
        }
    }

    /** {@code args[index]}, which must be there. */
    private static String argument(String[] args, int index) throws ArgumentException {
        if (index >= args.length) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
        return args[index];
    }
}
