package me.kous500.curvebuilding;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.Arrays;

/**
 * Messages of the game itself, by their translation keys, so that the player's client shows them in its own language
 * and in the words of the game's own command errors. Send them only to players: some platforms turn a message for
 * any other sender into plain text, which leaves the translated part out.
 */
public enum GameMessage {
    INCORRECT_ARGUMENT("command.unknown.argument"),
    /** Arguments: the lowest value allowed, and the value found. */
    INTEGER_TOO_LOW("argument.integer.low"),
    /** Argument: the value found. */
    INVALID_INTEGER("parsing.int.invalid"),
    /** "<--[HERE]", after the argument with the problem. */
    HERE("command.context.here");

    public final String key;

    GameMessage(String key) {
        this.key = key;
    }

    public TranslatableComponent text(Object... args) {
        return TranslatableComponent.of(key, Arrays.stream(args)
                .map(arg -> TextComponent.of(String.valueOf(arg)))
                .toArray(Component[]::new));
    }
}
