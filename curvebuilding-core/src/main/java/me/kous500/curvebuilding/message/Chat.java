package me.kous500.curvebuilding.message;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;

import static me.kous500.curvebuilding.message.Messages.getMessage;

/** Sends messages to a player through WorldEdit. */
public final class Chat {
    private Chat() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sends a message from the message file to {@code player}, in the color of WorldEdit's own messages.
     *
     * @param player the player to send it to
     * @param message the message
     * @param args the values for {@code {0}}, {@code {1}}, ... in the message
     */
    public static void info(Player player, Message message, Object... args) {
        player.printInfo(TextComponent.of(getMessage(message, args)));
    }

    /**
     * Sends a message from the message file to {@code player} as an error, in red.
     *
     * @param player the player to send it to
     * @param message the message
     * @param args the values for {@code {0}}, {@code {1}}, ... in the message
     */
    public static void error(Player player, Message message, Object... args) {
        player.printError(TextComponent.of(getMessage(message, args)));
    }

    /**
     * Sends a message of the game itself to {@code player} as an error, in red, in the language of their client.
     *
     * @param player the player to send it to
     * @param message the message
     * @param args the arguments of the message, in its order
     */
    public static void error(Player player, GameMessage message, Object... args) {
        player.printError(message.text(args));
    }

    /**
     * Shows {@code player} where a command has a problem, as the game shows its own command errors:
     * "...before arg&lt;--[HERE]".
     *
     * @param player the player to send it to
     * @param before the text in front of the argument, such as the argument before it
     * @param arg the argument with the problem, or an empty text when one is missing
     */
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
