package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.debugRun;
import static me.kous500.curvebuilding.fabric.network.PacketSender.sendPosPacket;
import static net.minecraft.commands.Commands.literal;

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

    public static void setPosBuilder(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String command : posCommandArg.keySet()) {
            LiteralArgumentBuilder<CommandSourceStack> builder;
            switch (posCommandArg.get(command)) {
                case 1 -> builder = literal(command).then(
                        Commands
                                .argument(T, StringArgumentType.string())
                                .executes(context -> debugRun(new Pos(context, command, S, T)))
                        );
                case 2 -> {
                    RequiredArgumentBuilder<CommandSourceStack, String> directionArg = Commands
                            .argument(D, StringArgumentType.string())
                            .executes(context -> debugRun(new Pos(context, command, S, L, D)));
                    builder = literal(command).then(
                            Commands
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
                            .requires(commandSource -> commandSource.hasPermission(2))
            );
        }
    }

    private final CommandContext<CommandSourceStack> context;
    private final String[] arg;

    public Pos(CommandContext<CommandSourceStack> context, String command, String... array) {
        this.context = context;
        this.arg = Arrays.stream(array)
                .map(type -> S.equals(type) ? command : String.valueOf(context.getArgument(type, argType.get(type))))
                .toArray(String[]::new);
    }

    @Override
    public int run() {
        ServerPlayer sender = context.getSource().getPlayer();
        if (sender == null) {
            return 0;
        }

        boolean result = posCommand(adaptPlayer(sender), posCommandName, arg);
        sendPosPacket(sender);

        return result ? 1 : 0;
    }

    @Override
    public CommandSourceStack getSource() {
        return context.getSource();
    }
}
