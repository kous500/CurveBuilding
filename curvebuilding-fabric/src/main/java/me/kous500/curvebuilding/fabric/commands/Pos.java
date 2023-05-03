package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static com.sk89q.worldedit.fabric.FabricAdapter.adaptPlayer;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.debugRun;
import static net.minecraft.server.command.CommandManager.literal;

public class Pos implements RunCommand{
    private static final String posCommandName = "/pos";
    private static final String posArgName = "PosType";

    private static final Map<String, Boolean> posCommandArg = new HashMap<>() {
        {
            put("set", true);
            put("clear", true);
            put("clearall", false);
        }
    };

    public static void setPosBuilder(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String command : posCommandArg.keySet()) {
            LiteralArgumentBuilder<ServerCommandSource> builder;
            if (posCommandArg.get(command)) {
                builder = literal(posCommandName).then(literal(command).then(
                        CommandManager
                                .argument(posArgName, StringArgumentType.string())
                                .executes(context -> debugRun(new Pos(context, command)))
                ));
            } else {
                builder = literal(posCommandName).then(literal(command)
                        .executes(context -> debugRun(new Pos(context, command))));
            }
            builder = builder.requires(commandSource -> commandSource.hasPermissionLevel(2));

            dispatcher.register(builder);
        }
    }

    private final CommandContext<ServerCommandSource> context;
    private final String command;

    public Pos(CommandContext<ServerCommandSource> context, String command) {
        this.context = context;
        this.command = command;
    }

    @Override
    public int run() {
        if (context.getSource().getPlayer() == null) {
            return 0;
        }

        String[] commandArg = posCommandArg.get(command)
                ? new String[]{command, StringArgumentType.getString(context, posArgName)} : new String[]{command};

        boolean result = posCommand(adaptPlayer(context.getSource().getPlayer()), posCommandName, commandArg);

        return result ? 1 : 0;
    }

    @Override
    public ServerCommandSource getSource() {
        return context.getSource();
    }
}
