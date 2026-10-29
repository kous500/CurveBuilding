package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.regex.Pattern;

import static me.kous500.curvebuilding.CurveBuilding.config;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.math.PosManager.*;

public class PosCommand {
    public static StringBuilder log = new StringBuilder();

    public static boolean posCommand(Player player, String command, String[] args) {
        if (!command.equalsIgnoreCase("/bcpos")) return false;
        if (!log.isEmpty()) player.printInfo(TextComponent.of(log.toString()));

        PosSubcommand subcommand = args.length >= 1 ? PosSubcommand.of(args[0]) : null;
        if (subcommand == null || (subcommand.arguments != PosSubcommand.Arguments.NONE && args.length < 2)) {
            player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
            if (args.length >= 1) problemHere(player, "bcpos", args[0]);
            else problemHere(player, "pos", "");
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
                    player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                    problemHere(player, args[0], args[1]);
                    return false;
                }

                String direction = args.length >= 3 ? args[2] : "me";
                Pattern regex = Pattern.compile("^[bdeflmnrsuw]", Pattern.CASE_INSENSITIVE);
                if (!regex.matcher(direction).find()) {
                    player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                    problemHere(player, args[1], args[2]);
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
                player.printError(TextComponent.of(getMessage("messages.integer-less", 1, args[1])));
                problemHere(player, args[0], args[1]);
            } case incorrectArgument -> {
                player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                problemHere(player, args[0], args[1]);
            } case invalidInteger -> {
                player.printError(TextComponent.of(getMessage("messages.invalid-integer", args[1])));
                problemHere(player, args[0], args[1]);
            } case maxPosValue -> {
                player.printError(TextComponent.of(getMessage("messages.max-pos-value", config.maxPosValue)));
                problemHere(player, args[0], args[1]);
            }
        }

        return false;
    }

    public static void problemHere(Player player, String beforeArg, String arg) {
        player.printInfo(TextComponent.of(
                "§7..."+beforeArg+"§c §c§n"+arg+"§c§o" + getMessage("messages.problem-here")));
    }
}
