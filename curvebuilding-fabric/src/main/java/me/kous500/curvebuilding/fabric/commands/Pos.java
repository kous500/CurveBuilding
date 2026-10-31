package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import me.kous500.curvebuilding.commands.pos.PosCompletion;
import me.kous500.curvebuilding.commands.pos.PosSubcommand;
import me.kous500.curvebuilding.fabric.FabricCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static me.kous500.curvebuilding.fabric.FabricCompat.adaptPlayer;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.runLogged;
import static me.kous500.curvebuilding.fabric.network.PacketSender.sendPosPacket;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Pos implements RunCommand{
    private static final String posCommandName = "/bcpos";
    private static final String T = "PosType";
    private static final String L = "Length";
    private static final String D = "Direction";

    private static final Map<String, Class<?>> argType = new HashMap<>() {
        {
            put(T, String.class);
            put(L, int.class);
            put(D, String.class);
        }
    };

    private static final SuggestionProvider<CommandSourceStack> suggestions = CoreSuggestions.of(PosCompletion::complete);

    public static void setPosBuilder(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> pos = literal(posCommandName).requires(FabricCompat::isGameMaster);

        for (PosSubcommand subcommand : PosSubcommand.values()) {
            String label = subcommand.label;
            LiteralArgumentBuilder<CommandSourceStack> builder = literal(label);
            switch (subcommand.arguments) {
                case NONE -> builder.executes(context -> runLogged(new Pos(context, label)));
                case POS_TYPE, POINT -> builder.then(argument(T, string())
                        .suggests(suggestions)
                        .executes(context -> runLogged(new Pos(context, label, T))));
                case LENGTH_AND_DIRECTION -> builder.then(argument(L, integer(0))
                        .suggests(suggestions)
                        .executes(context -> runLogged(new Pos(context, label, L)))
                        .then(argument(D, string())
                                .executes(context -> runLogged(new Pos(context, label, L, D)))));
            }
            pos.then(builder);
        }

        dispatcher.register(pos);
    }

    private final CommandContext<CommandSourceStack> context;
    private final String[] arg;

    public Pos(CommandContext<CommandSourceStack> context, String subcommand, String... arguments) {
        this.context = context;
        this.arg = Stream.concat(
                Stream.of(subcommand),
                Stream.of(arguments).map(type -> String.valueOf(context.getArgument(type, argType.get(type))))
        ).toArray(String[]::new);
    }

    @Override
    public int run() throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();

        boolean result = posCommand(adaptPlayer(sender), posCommandName, arg);
        sendPosPacket(sender);

        return result ? 1 : 0;
    }

    @Override
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }
}
