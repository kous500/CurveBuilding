package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.kous500.curvebuilding.commands.bc.BcCompletion;
import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.kous500.curvebuilding.commands.bc.BcCommand.bcCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.runLogged;
import static me.kous500.curvebuilding.fabric.compat.FabricCompat.adaptPlayer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * The command {@code //bc} on Fabric.
 */
public class Bc implements RunCommand {
    private static final String bcCommandName = "/bc";
    private static final Command<CommandSourceStack> RUN = context -> runLogged(new Bc(context));

    /**
     * Registers {@code //bc}.
     *
     * @param dispatcher the dispatcher to register it with
     */
    public static void setBcBuilder(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, Integer> offset = argument("offset", integer()).executes(RUN);

        dispatcher.register(literal(bcCommandName)
                .then(argument("options", word())
                        // Only here: the options come before the offset in this command tree
                        .suggests(CoreSuggestions.of(BcCompletion::complete))
                        .then(offset)
                        .executes(RUN))
                .then(offset)
                .executes(RUN)
                .requires(FabricCompat::isGameMaster));
    }

    private final CommandContext<CommandSourceStack> context;

    private Bc(CommandContext<CommandSourceStack> context) {
        this.context = context;
    }

    @Override
    public int run() throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        return bcCommand(adaptPlayer(sender), CommandInput.arguments(context)) ? 1 : 0;
    }

    @Override
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }
}
