package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.commands.IntegerArgument;

import static me.kous500.curvebuilding.commands.CommandArguments.read;
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
        try {
            PosSubcommand subcommand = read(args, 0, PosSubcommand::parse);
            int end = 1 + subcommand.arguments.max;
            if (args.length > end) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(end);

            switch (subcommand) {
                case CLEARALL -> clearPos(player);
                case CLEAR -> {
                    PosPoint point = read(args, 1, PosPoint::parse);
                    clearPos(player, point.n(), point.part());
                }
                case SET -> {
                    PosPoint point = read(args, 1, PosPoint::parse);
                    addPos(player, point.n(), point.part());
                }
                case INSERT -> insert(player, read(args, 1, PosPoint::parseNumber));
                case REMOVE -> remove(player, read(args, 1, PosPoint::parseNumber));
                case SHIFT -> {
                    int length = read(args, 1, text -> IntegerArgument.parse(text, 0));
                    Direction direction = args.length > 2 ? read(args, 2, Direction::parse) : Direction.ME;
                    shift(player, length, direction);
                }
            }
            return true;
        } catch (ArgumentException e) {
            e.show(player, "bcpos", args);
            return false;
        }
    }
}
