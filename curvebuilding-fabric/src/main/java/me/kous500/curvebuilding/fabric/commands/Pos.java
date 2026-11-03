package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.kous500.curvebuilding.commands.pos.PosCompletion;
import me.kous500.curvebuilding.commands.pos.PosSubcommand;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.runLogged;
import static me.kous500.curvebuilding.fabric.compat.FabricCompat.adaptPlayer;
import static me.kous500.curvebuilding.fabric.network.PacketSender.sendPosPacket;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * The command {@code //bcpos} on Fabric.
 */
public class Pos implements RunCommand {
    private static final String posCommandName = "/bcpos";
    private static final SuggestionProvider<CommandSourceStack> suggestions =
            CoreSuggestions.of(PosCompletion::complete);

    /**
     * Registers {@code //bcpos}, with a node for each subcommand.
     *
     * @param dispatcher the dispatcher to register it with
     * @param config gives the config in use each time the command runs
     */
    public static void setPosBuilder(CommandDispatcher<CommandSourceStack> dispatcher, Supplier<Config> config) {
        Command<CommandSourceStack> run = context -> runLogged(new Pos(context, config.get()));
        LiteralArgumentBuilder<CommandSourceStack> pos = literal(posCommandName).requires(FabricCompat::isGameMaster);

        for (PosSubcommand subcommand : PosSubcommand.values()) {
            LiteralArgumentBuilder<CommandSourceStack> builder = literal(subcommand.label);
            // word() rather than string(), which also takes quoted text, so that the input is what core reads
            switch (subcommand.arguments) {
                case NONE -> builder.executes(run);
                case POS_TYPE, POINT -> builder.then(argument("pos", word()).suggests(suggestions).executes(run));
                case LENGTH_AND_DIRECTION -> builder.then(argument("length", integer(0))
                        .suggests(suggestions)
                        .executes(run)
                        .then(argument("direction", word()).executes(run)));
            }
            pos.then(builder);
        }

        dispatcher.register(pos);
    }

    private final CommandContext<CommandSourceStack> context;
    private final Config config;

    private Pos(CommandContext<CommandSourceStack> context, Config config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public int run() throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();

        boolean result = posCommand(adaptPlayer(sender), CommandInput.arguments(context), config);
        sendPosPacket(sender);

        return result ? 1 : 0;
    }

    @Override
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }
}
