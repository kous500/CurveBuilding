package me.kous500.curvebuilding.commands;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.config.Message;

import java.util.List;

/** A command argument that cannot be used, with the error message to show for it. */
public final class ArgumentException extends Exception {
    private final Enum<?> message;
    private final transient Object[] args;
    private int index = -1;

    public ArgumentException(GameMessage message, Object... args) {
        super(message.key, null, false, false);
        this.message = message;
        this.args = args;
    }

    public ArgumentException(Message message, Object... args) {
        super(message.path, null, false, false);
        this.message = message;
        this.args = args;
    }

    /** The {@link GameMessage} or {@link Message} to show. */
    public Enum<?> message() {
        return message;
    }

    public List<Object> args() {
        return List.of(args);
    }

    /** Makes this the error of the argument at {@code index}, unless it is already the error of one. */
    public ArgumentException at(int index) {
        if (this.index < 0) this.index = index;
        return this;
    }

    /** The index of the argument with the problem, or -1 when no argument was set with {@link #at}. */
    public int index() {
        return index;
    }

    /**
     * Shows the error, then points at the argument with the problem, or at the end of the command when it is missing.
     *
     * @param command the command name, shown in front of the first argument
     */
    public void show(Player player, String command, String[] args) {
        int index = Math.max(this.index, 0);
        if (message instanceof GameMessage gameMessage) {
            Chat.error(player, gameMessage, this.args);
        } else {
            Chat.error(player, (Message) message, this.args);
        }
        Chat.problemHere(player, index == 0 ? command : args[index - 1], index < args.length ? args[index] : "");
    }
}
