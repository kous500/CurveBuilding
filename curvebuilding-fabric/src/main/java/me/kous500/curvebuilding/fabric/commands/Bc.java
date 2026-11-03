package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.kous500.curvebuilding.commands.bc.BcCompletion;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

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

    /**
     * Registers {@code //bc}.
     *
     * @param dispatcher the dispatcher to register it with
     * @param config gives the config in use each time the command runs
     */
    public static void setBcBuilder(CommandDispatcher<CommandSourceStack> dispatcher, Supplier<Config> config) {
        Command<CommandSourceStack> run = context -> runLogged(new Bc(context, config.get()));
        RequiredArgumentBuilder<CommandSourceStack, Integer> offset = argument("offset", integer()).executes(run);

        dispatcher.register(literal(bcCommandName)
                .then(argument("options", word())
                        // Only here: the options come before the offset in this command tree
                        .suggests(CoreSuggestions.of(BcCompletion::complete))
                        .then(offset)
                        .executes(run))
                .then(offset)
                .executes(run)
                .requires(FabricCompat::isGameMaster));
    }

    private final CommandContext<CommandSourceStack> context;
    private final Config config;

    private Bc(CommandContext<CommandSourceStack> context, Config config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public int run() throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        return bcCommand(adaptPlayer(sender), CommandInput.arguments(context), config) ? 1 : 0;
    }

    @Override
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }
}
