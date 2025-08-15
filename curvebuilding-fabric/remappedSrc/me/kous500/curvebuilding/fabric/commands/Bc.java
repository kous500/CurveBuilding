package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.commands.bc.BcCommand;
import me.kous500.curvebuilding.commands.bc.BcEdit;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.debugRun;
import static net.minecraft.server.command.CommandManager.literal;

public class Bc implements RunCommand {
    private static final String bcCommandName = "/bc";
    private static final String options = "options";
    private static final String n = "n";
    private static final String m = "m";

    private static final Map<String, Class<?>> argType = new HashMap<>() {
        {
            put(options, String.class);
            put(n, int.class);
            put(m, int.class);
        }
    };

    public static void setBcBuilder(CommandDispatcher<ServerCommandSource> dispatcher) {
        RequiredArgumentBuilder<ServerCommandSource, Integer> builderM = CommandManager
                .argument(m, integer())
                .executes(context -> debugRun(new Bc(context, options, n, m)));

        RequiredArgumentBuilder<ServerCommandSource, Integer> builderN = CommandManager
                .argument(n, integer())
                .then(builderM)
                .executes(context -> debugRun(new Bc(context, options, n)));

        RequiredArgumentBuilder<ServerCommandSource, String> builderOption = CommandManager
                .argument(options, string())
                .then(builderN)
                .executes(context -> debugRun(new Bc(context, options)));

        LiteralArgumentBuilder<ServerCommandSource> builder = literal(bcCommandName)
                .then(builderOption)
                .then(builderN)
                .executes(context -> debugRun(new Bc(context)))
                .requires(commandSource -> commandSource.hasPermissionLevel(2));

        dispatcher.register(builder);
    }

    private final CommandContext<ServerCommandSource> context;

    private final String[] arg;

    public Bc(CommandContext<ServerCommandSource> context, String... array) {
        this.context = context;

        this.arg = Arrays.stream(array)
                .map(type -> String.valueOf(context.getArgument(type, argType.get(type))))
                .toArray(String[]::new);
    }

    @Override
    public int run() {
        ServerPlayerEntity serverPlayer = context.getSource().getPlayer();
        if (serverPlayer == null) return 0;

        Player player = adaptPlayer(serverPlayer);
        BcCommand argument = new BcCommand(arg, player);

        if (!argument.success) return 0;
        new BcEdit(player, argument);

        return 1;
    }

    @Override
    public ServerCommandSource getSource() {
        return context.getSource();
    }
}
