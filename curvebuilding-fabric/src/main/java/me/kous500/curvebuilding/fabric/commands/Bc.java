package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.kous500.curvebuilding.commands.bc.BcCompletion;
import me.kous500.curvebuilding.fabric.FabricCompat;
import net.minecraft.commands.CommandSourceStack;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.kous500.curvebuilding.commands.bc.BcCommand.bcCommand;
import static me.kous500.curvebuilding.fabric.FabricCompat.adaptPlayer;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.runLogged;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Bc implements RunCommand {
    private static final String bcCommandName = "/bc";
    private static final Command<CommandSourceStack> RUN = context -> runLogged(new Bc(context));

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
        return bcCommand(adaptPlayer(context.getSource().getPlayerOrException()), CommandInput.arguments(context)) ? 1 : 0;
    }

    @Override
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }
}
