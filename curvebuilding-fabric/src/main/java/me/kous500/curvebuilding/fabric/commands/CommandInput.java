package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

/** The arguments typed after a command name, split at the spaces, as Bukkit passes them to a command. */
final class CommandInput {
    private CommandInput() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** The arguments of the command being run, as typed; core reads them, not the values Brigadier parsed. */
    static String[] arguments(CommandContext<CommandSourceStack> context) {
        int commandEnd = context.getNodes().get(0).getRange().getEnd();
        return split(context.getInput().substring(commandEnd));
    }

    static String[] split(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split(" +");
    }
}
