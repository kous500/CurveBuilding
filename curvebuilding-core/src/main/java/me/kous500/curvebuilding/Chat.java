package me.kous500.curvebuilding;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import me.kous500.curvebuilding.config.Message;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;

/** Sends messages to a player through WorldEdit. */
public final class Chat {
    private Chat() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void info(Player player, Message message, Object... args) {
        player.printInfo(TextComponent.of(getMessage(message, args)));
    }

    public static void error(Player player, Message message, Object... args) {
        player.printError(TextComponent.of(getMessage(message, args)));
    }

    /** Points at {@code arg}, the argument with the problem, shown after {@code before}, the text in front of it. */
    public static void problemHere(Player player, String before, String arg) {
        player.printInfo(TextComponent.of("§7..." + before + "§c §c§n" + arg + "§c§o" + getMessage(Message.PROBLEM_HERE)));
    }
}
