package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.commands.IntegerArgument;
import me.kous500.curvebuilding.curve.CurvePoint.Part;
import me.kous500.curvebuilding.curve.PosManager;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.message.Chat;
import me.kous500.curvebuilding.message.GameMessage;
import me.kous500.curvebuilding.message.Message;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.commands.CommandArguments.read;

/**
 * Runs {@code //bcpos} on every platform, and replies to the player.
 */
public final class PosCommand {
    private PosCommand() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Runs {@code //bcpos} with {@code args}, or shows the error of the first argument that cannot be used.
     *
     * @param player the player who ran the command
     * @param args the arguments of the command
     * @return whether the arguments could be used
     */
    public static boolean posCommand(Player player, String[] args) {
        try {
            PosSubcommand subcommand = read(args, 0, PosSubcommand::parse);
            int end = 1 + subcommand.arguments.max;
            if (args.length > end) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(end);

            switch (subcommand) {
                case CLEARALL -> {
                    if (PosManager.clearPos(player)) Chat.info(player, Message.POS_CLEAR_ALL);
                }
                case CLEAR -> {
                    PosPoint point = read(args, 1, PosPoint::parse);
                    if (PosManager.clearPos(player, point.n(), point.part())) {
                        Chat.info(player, Message.POS_CLEAR, posToString(point.n(), point.part()));
                    }
                }
                case SET -> {
                    PosPoint point = read(args, 1, PosPoint::parse);
                    set(player, point.n(), point.part());
                }
                case INSERT -> {
                    int n = read(args, 1, PosPoint::parseNumber);
                    if (PosManager.insertAt(player, n)) {
                        set(player, n, Part.POINT);
                    } else {
                        Chat.error(player, Message.POS_NOT_EXIST, n);
                    }
                }
                case REMOVE -> {
                    int n = read(args, 1, PosPoint::parseNumber);
                    if (PosManager.remove(player, n)) {
                        Chat.info(player, Message.POS_REMOVE, n);
                    } else {
                        Chat.error(player, Message.POS_NOT_EXIST, n);
                    }
                }
                case SHIFT -> {
                    int length = read(args, 1, text -> IntegerArgument.parse(text, 0));
                    Direction direction = args.length > 2 ? read(args, 2, Direction::parse) : Direction.ME;
                    Location location = player.getLocation();
                    Vector3 offset = direction.offset(length, location.getYaw(), location.getPitch());
                    if (PosManager.move(player, offset)) Chat.info(player, Message.POS_SHIFT);
                }
            }
            return true;
        } catch (ArgumentException e) {
            e.show(player, "bcpos", args);
            return false;
        }
    }

    private static void set(Player player, int n, Part part) {
        Vector3 position = PosManager.addPos(player, n, part);
        Chat.info(player, Message.POS_SET, posToString(n, part), position);
    }

    private static String posToString(int n, Part part) {
        return switch (part) {
            case F -> getMessage(Message.POS_NF, n);
            case B -> getMessage(Message.POS_NB, n);
            case POINT -> getMessage(Message.POS_N, n);
        };
    }
}
