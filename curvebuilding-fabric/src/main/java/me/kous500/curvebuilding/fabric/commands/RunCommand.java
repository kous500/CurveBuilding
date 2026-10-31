package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

import static me.kous500.curvebuilding.CurveBuilding.console;

public interface RunCommand {
    int run() throws CommandSyntaxException;

    CommandContext<CommandSourceStack> getContext();

    /**
     * Runs {@code command} and logs an unexpected exception with its stack trace. The game then only tells the sender
     * that the command failed, and logs the exception itself only when run from an IDE or with debug logging.
     */
    static int runLogged(RunCommand command) throws CommandSyntaxException {
        try {
            return command.run();
        } catch (RuntimeException e) {
            console.warning("Could not run /" + command.getContext().getInput(), e);
            throw e;
        }
    }
}
