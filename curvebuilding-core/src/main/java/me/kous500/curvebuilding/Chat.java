package me.kous500.curvebuilding;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;
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

    public static void error(Player player, GameMessage message, Object... args) {
        player.printError(message.text(args));
    }

    /** Points at {@code arg}, the argument with the problem, shown after {@code before}, the text in front of it. */
    public static void problemHere(Player player, String before, String arg) {
        player.printInfo(problemHereText(before, arg));
    }

    /** "...before arg&lt;--[HERE]", in the colors of the game's own command errors. */
    static Component problemHereText(String before, String arg) {
        return TextComponent.builder("")
                .append(TextComponent.of("..." + before, TextColor.GRAY))
                .append(TextComponent.of(" ", TextColor.RED))
                .append(TextComponent.of(arg, TextColor.RED, TextDecoration.UNDERLINED))
                .append(GameMessage.HERE.text().color(TextColor.RED).decoration(TextDecoration.ITALIC, true))
                .build();
    }
}
