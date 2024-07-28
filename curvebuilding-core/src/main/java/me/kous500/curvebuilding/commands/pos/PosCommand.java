package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.regex.Pattern;

import static me.kous500.curvebuilding.CurveBuilding.config;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.math.PosData.*;

public class PosCommand {
    public static StringBuilder log = new StringBuilder();

    public static boolean posCommand(Player player, String command, String[] args) {
        if (!command.equalsIgnoreCase("/bcpos")) return false;
        if (!log.isEmpty()) player.printInfo(TextComponent.of(log.toString()));

        if (args.length >= 2 && args[0].equals("clear")) {
            PosType posType = PosType.get(args[1]);
            if (argIsError(player, args, posType)) {
                clearPos(player, posType.n, posType.h);
                return true;
            } else {
                return false;
            }
        } else if (args.length >= 1 && args[0].equals("clearall")) {
            clearPos(player);
            return true;
        } else if (args.length >= 2 && args[0].equals("insert")) {
            PosType posType = PosType.get(args[1]);
            if (argIsError(player, args, posType)) {
                insert(player, posType.n);
                return true;
            } else {
                return false;
            }
        } else if (args.length >= 2 && args[0].equals("remove")) {
            PosType posType = PosType.get(args[1]);
            if (argIsError(player, args, posType)) {
                remove(player, posType.n);
                return true;
            } else {
                return false;
            }
        } else if (args.length >= 2 && args[0].equals("set")) {
            PosType posType = PosType.get(args[1]);
            if (argIsError(player, args, posType)) {
                addPos(player, posType.n, posType.h);
                return true;
            } else {
                return false;
            }
        } else if (args.length >= 2 && args[0].equals("shift")) {
            int n = toInt(args[1]);
            if (n >= 0) {
                String direction = args.length >= 3 ? args[2] : "me";
                Pattern regex = Pattern.compile("^[bdeflmnrsuw]", Pattern.CASE_INSENSITIVE);
                if (!regex.matcher(direction).find()) {
                    player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                    problemHere(player, args[1], args[2]);
                    return false;
                }

                shift(player, n, direction.charAt(0));
                return true;
            } else {
                player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                problemHere(player, args[0], args[1]);
                return false;
            }
        } else {
            player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
            if (args.length >= 1) problemHere(player, "bcpos", args[0]);
            else problemHere(player, "pos", "");
            return false;
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
