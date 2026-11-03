package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

import static me.kous500.curvebuilding.log.Log.console;

/**
 * A run of a command of CurveBuilding, with the context it was typed in.
 */
public interface RunCommand {
    /**
     * Runs the command.
     *
     * @return 1 when the command did what it was asked, 0 otherwise
     * @throws CommandSyntaxException when the sender is not a player
     */
    int run() throws CommandSyntaxException;

    /**
     * The context the command was typed in.
     *
     * @return the context
     */
    CommandContext<CommandSourceStack> getContext();

    /**
     * Runs {@code command} and logs an unexpected exception with its stack trace. The game then only tells the sender
     * that the command failed, and logs the exception itself only when run from an IDE or with debug logging.
     *
     * @param command the command to run
     * @return what {@link #run} returns
     * @throws CommandSyntaxException when the sender is not a player
     */
    static int runLogged(RunCommand command) throws CommandSyntaxException {
        try {
            return command.run();
        } catch (RuntimeException e) {
            console().warning("Could not run /" + command.getContext().getInput(), e);
            throw e;
        }
    }
}
