package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.debugRun;
import static me.kous500.curvebuilding.fabric.network.PacketSender.sendPosPacket;
import static net.minecraft.server.command.CommandManager.literal;

public class Pos implements RunCommand{
    private static final String posCommandName = "/bcpos";
    private static final String S = "SubCommand";
    private static final String T = "PosType";
    private static final String L = "Length";
    private static final String D = "Direction";


    private static final Map<String, Integer> posCommandArg = new HashMap<>() {
        {
            put("set", 1);
            put("clear", 1);
            put("clearall", 0);
            put("insert", 1);
            put("remove", 1);
            put("shift", 2);
        }
    };

    private static final Map<String, Class<?>> argType = new HashMap<>() {
        {
            put(T, String.class);
            put(L, int.class);
            put(D, String.class);
        }
    };

    public static void setPosBuilder(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String command : posCommandArg.keySet()) {
            LiteralArgumentBuilder<ServerCommandSource> builder;
            switch (posCommandArg.get(command)) {
                case 1 -> builder = literal(command).then(
                        CommandManager
                                .argument(T, StringArgumentType.string())
                                .executes(context -> debugRun(new Pos(context, command, S, T)))
                        );
                case 2 -> {
                    RequiredArgumentBuilder<ServerCommandSource, String> directionArg = CommandManager
                            .argument(D, StringArgumentType.string())
                            .executes(context -> debugRun(new Pos(context, command, S, L, D)));
                    builder = literal(command).then(
                            CommandManager
                                    .argument(L, IntegerArgumentType.integer(0))
                                    .executes(context -> debugRun(new Pos(context, command, S, L)))
                                    .then(directionArg)
                    );
                }
                default -> builder = literal(command)
                        .executes(context -> debugRun(new Pos(context, command, S)));
            }

            dispatcher.register(
                    literal(posCommandName)
                            .then(builder)
                            .requires(commandSource -> commandSource.hasPermissionLevel(2))
            );
        }
    }

    private final CommandContext<ServerCommandSource> context;
    private final String[] arg;

    public Pos(CommandContext<ServerCommandSource> context, String command, String... array) {
        this.context = context;
        this.arg = Arrays.stream(array)
                .map(type -> S.equals(type) ? command : String.valueOf(context.getArgument(type, argType.get(type))))
                .toArray(String[]::new);
    }

    @Override
    public int run() {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        if (sender == null) {
            return 0;
        }

        boolean result = posCommand(adaptPlayer(sender), posCommandName, arg);
        sendPosPacket(sender);

        return result ? 1 : 0;
    }

    @Override
    public ServerCommandSource getSource() {
        return context.getSource();
    }
}
