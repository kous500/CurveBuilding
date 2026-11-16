package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.commands.IntegerArgument;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.curve.CurvePoint.Part;
import me.kous500.curvebuilding.curve.PosManager;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.message.Chat;
import me.kous500.curvebuilding.message.GameMessage;
import me.kous500.curvebuilding.message.Message;

import static me.kous500.curvebuilding.WorldeditAdapter.adapt;
import static me.kous500.curvebuilding.commands.CommandArguments.read;
import static me.kous500.curvebuilding.message.Messages.getMessage;

/**
 * Runs {@code //bcpos} on every platform, and replies to the player.
 */
public final class PosCommand {
    /** The index of the x coordinate in the arguments of {@code set} and {@code insert}. */
    private static final int COORDINATES = 2;

    private PosCommand() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Runs {@code //bcpos} with {@code args}, or shows the error of the first argument that cannot be used.
     *
     * @param player the player who ran the command
     * @param args the arguments of the command
     * @param config the config in use
     * @return whether the arguments could be used
     */
    public static boolean posCommand(Player player, String[] args, Config config) {
        int maxPosValue = config.maxPosValue;
        try {
            PosSubcommand subcommand = read(args, 0, PosSubcommand::parse);
            int end = 1 + subcommand.arguments.max;
            if (args.length > end) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT).at(end);

            switch (subcommand) {
                case CLEARALL -> {
                    if (PosManager.clearPos(player)) Chat.info(player, Message.POS_CLEAR_ALL);
                }
                case CLEAR -> {
                    PosPoint point = read(args, 1, text -> PosPoint.parse(text, maxPosValue));
                    if (PosManager.clearPos(player, point.n(), point.part())) {
                        Chat.info(player, Message.POS_CLEAR, posToString(point.n(), point.part()));
                    }
                }
                case SET -> {
                    PosPoint point = read(args, 1, text -> PosPoint.parse(text, maxPosValue));
                    set(player, point.n(), point.part(), position(player, args));
                }
                case INSERT -> {
                    int n = read(args, 1, text -> PosPoint.parseNumber(text, maxPosValue));
                    // Read before inserting, so that coordinates that cannot be used change nothing
                    Vector3 position = position(player, args);
                    if (PosManager.insertAt(player, n)) {
                        set(player, n, Part.POINT, position);
                    } else {
                        Chat.error(player, Message.POS_NOT_EXIST, n);
                    }
                }
                case REMOVE -> {
                    int n = read(args, 1, text -> PosPoint.parseNumber(text, maxPosValue));
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

    private static void set(Player player, int n, Part part, Vector3 position) {
        PosManager.addPos(player, n, part, position);
        Chat.info(player, Message.POS_SET, posToString(n, part), position);
    }

    /**
     * The position at the coordinates after the point, as in {@code set 2 x y z}, or where the player stands when there
     * are none.
     */
    private static Vector3 position(Player player, String[] args) throws ArgumentException {
        Location location = player.getLocation();
        Vector3 feet = adapt(location.toVector());
        if (args.length <= COORDINATES) return PosManager.standingPosition(feet);
        return PosCoordinates.parse(args, COORDINATES).position(feet, location.getYaw(), location.getPitch());
    }

    private static String posToString(int n, Part part) {
        return switch (part) {
            case F -> getMessage(Message.POS_NF, n);
            case B -> getMessage(Message.POS_NB, n);
            case POINT -> getMessage(Message.POS_N, n);
        };
    }
}
