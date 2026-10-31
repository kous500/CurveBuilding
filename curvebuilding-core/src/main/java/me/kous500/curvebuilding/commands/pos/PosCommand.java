package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.config.Message;

import java.util.regex.Pattern;

import static me.kous500.curvebuilding.CurveBuilding.config;
import static me.kous500.curvebuilding.math.PosManager.*;

public class PosCommand {
    public static boolean posCommand(Player player, String command, String[] args) {
        if (!command.equalsIgnoreCase("/bcpos")) return false;

        PosSubcommand subcommand = args.length >= 1 ? PosSubcommand.of(args[0]) : null;
        if (subcommand == null || (subcommand.arguments != PosSubcommand.Arguments.NONE && args.length < 2)) {
            Chat.error(player, Message.INCORRECT_ARGUMENT);
            if (args.length >= 1) Chat.problemHere(player, "bcpos", args[0]);
            else Chat.problemHere(player, "pos", "");
            return false;
        }

        switch (subcommand) {
            case CLEARALL -> {
                clearPos(player);
                return true;
            }
            case SHIFT -> {
                int n = toInt(args[1]);
                if (n < 0) {
                    Chat.error(player, Message.INCORRECT_ARGUMENT);
                    Chat.problemHere(player, args[0], args[1]);
                    return false;
                }

                String direction = args.length >= 3 ? args[2] : "me";
                Pattern regex = Pattern.compile("^[bdeflmnrsuw]", Pattern.CASE_INSENSITIVE);
                if (!regex.matcher(direction).find()) {
                    Chat.error(player, Message.INCORRECT_ARGUMENT);
                    Chat.problemHere(player, args[1], args[2]);
                    return false;
                }

                shift(player, n, direction.charAt(0));
                return true;
            }
            default -> {
                PosType posType = PosType.get(args[1]);
                if (!argIsError(player, args, posType)) return false;

                switch (subcommand) {
                    case CLEAR -> clearPos(player, posType.n, posType.h);
                    case INSERT -> insert(player, posType.n);
                    case REMOVE -> remove(player, posType.n);
                    case SET -> addPos(player, posType.n, posType.h);
                    default -> throw new IllegalStateException("Unexpected subcommand: " + subcommand);
                }
                return true;
            }
        }
    }

    private static int toInt(String string) {
        int result;
        try {
            result = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            result = -1;
        }
        return result;
    }

    public static boolean argIsError(Player player, String[] args, PosType posType) {
        PosType.ErrorType errorType = posType.errorType;
        if (errorType == null) return true;

        switch (errorType) {
            case integerLess -> {
                Chat.error(player, Message.INTEGER_LESS, 1, args[1]);
                Chat.problemHere(player, args[0], args[1]);
            } case incorrectArgument -> {
                Chat.error(player, Message.INCORRECT_ARGUMENT);
                Chat.problemHere(player, args[0], args[1]);
            } case invalidInteger -> {
                Chat.error(player, Message.INVALID_INTEGER, args[1]);
                Chat.problemHere(player, args[0], args[1]);
            } case maxPosValue -> {
                Chat.error(player, Message.MAX_POS_VALUE, config.maxPosValue);
                Chat.problemHere(player, args[0], args[1]);
            }
        }

        return false;
    }
}
