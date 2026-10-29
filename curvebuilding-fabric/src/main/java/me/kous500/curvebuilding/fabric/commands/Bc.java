package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.fabric.FabricCompat;
import me.kous500.curvebuilding.commands.bc.BcCommand;
import me.kous500.curvebuilding.commands.bc.BcCompletion;
import me.kous500.curvebuilding.commands.bc.BcEdit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static me.kous500.curvebuilding.fabric.FabricCompat.adaptPlayer;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.debugRun;
import static net.minecraft.commands.Commands.literal;

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

    public static void setBcBuilder(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, Integer> builderM = Commands
                .argument(m, integer())
                .executes(context -> debugRun(new Bc(context, options, n, m)));

        RequiredArgumentBuilder<CommandSourceStack, Integer> builderN = Commands
                .argument(n, integer())
                .then(builderM)
                .executes(context -> debugRun(new Bc(context, options, n)));

        RequiredArgumentBuilder<CommandSourceStack, String> builderOption = Commands
                .argument(options, string())
                // Only here: the options come before the numbers in this command tree
                .suggests(CoreSuggestions.of(BcCompletion::complete))
                .then(builderN)
                .executes(context -> debugRun(new Bc(context, options)));

        LiteralArgumentBuilder<CommandSourceStack> builder = literal(bcCommandName)
                .then(builderOption)
                .then(builderN)
                .executes(context -> debugRun(new Bc(context)))
                .requires(FabricCompat::isGameMaster);

        dispatcher.register(builder);
    }

    private final CommandContext<CommandSourceStack> context;

    private final String[] arg;

    public Bc(CommandContext<CommandSourceStack> context, String... array) {
        this.context = context;

        this.arg = Arrays.stream(array)
                .map(type -> String.valueOf(context.getArgument(type, argType.get(type))))
                .toArray(String[]::new);
    }

    @Override
    public int run() {
        ServerPlayer serverPlayer = context.getSource().getPlayer();
        if (serverPlayer == null) return 0;

        Player player = adaptPlayer(serverPlayer);
        BcCommand argument = new BcCommand(arg, player);

        if (!argument.success) return 0;
        new BcEdit(player, argument);

        return 1;
    }

    @Override
    public CommandSourceStack getSource() {
        return context.getSource();
    }
}
