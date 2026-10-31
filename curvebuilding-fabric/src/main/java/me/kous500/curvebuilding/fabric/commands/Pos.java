package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.kous500.curvebuilding.commands.pos.PosCompletion;
import me.kous500.curvebuilding.commands.pos.PosSubcommand;
import me.kous500.curvebuilding.fabric.FabricCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.kous500.curvebuilding.fabric.FabricCompat.adaptPlayer;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.runLogged;
import static me.kous500.curvebuilding.fabric.network.PacketSender.sendPosPacket;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Pos implements RunCommand {
    private static final String posCommandName = "/bcpos";
    private static final Command<CommandSourceStack> RUN = context -> runLogged(new Pos(context));
    private static final SuggestionProvider<CommandSourceStack> suggestions = CoreSuggestions.of(PosCompletion::complete);

    public static void setPosBuilder(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> pos = literal(posCommandName).requires(FabricCompat::isGameMaster);

        for (PosSubcommand subcommand : PosSubcommand.values()) {
            LiteralArgumentBuilder<CommandSourceStack> builder = literal(subcommand.label);
            // word() rather than string(), which also takes quoted text, so that the input is what core reads
            switch (subcommand.arguments) {
                case NONE -> builder.executes(RUN);
                case POS_TYPE, POINT -> builder.then(argument("pos", word()).suggests(suggestions).executes(RUN));
                case LENGTH_AND_DIRECTION -> builder.then(argument("length", integer(0))
                        .suggests(suggestions)
                        .executes(RUN)
                        .then(argument("direction", word()).executes(RUN)));
            }
            pos.then(builder);
        }

        dispatcher.register(pos);
    }

    private final CommandContext<CommandSourceStack> context;

    private Pos(CommandContext<CommandSourceStack> context) {
        this.context = context;
    }

    @Override
    public int run() throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();

        boolean result = posCommand(adaptPlayer(sender), CommandInput.arguments(context));
        sendPosPacket(sender);

        return result ? 1 : 0;
    }

    @Override
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }
}
