package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.kous500.curvebuilding.commands.config.ConfigCommand;
import me.kous500.curvebuilding.commands.config.ConfigCommand.Reply;
import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import me.kous500.curvebuilding.message.Chat;
import me.kous500.curvebuilding.message.Message;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.kous500.curvebuilding.fabric.commands.RunCommand.runLogged;
import static me.kous500.curvebuilding.fabric.compat.FabricCompat.adaptPlayer;
import static me.kous500.curvebuilding.fabric.network.PacketSender.sendPosPacket;
import static me.kous500.curvebuilding.message.Messages.getMessage;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * The command {@code /curvebuilding} on Fabric, which reloads the files with {@code reload} and shows or sets a value
 * of config.yml with {@code config}.
 */
public final class CurveBuildingCommand {
    private CurveBuildingCommand() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Registers {@code /curvebuilding}. After the files are read again, every player's client is sent the positions
     * read.
     *
     * @param dispatcher the dispatcher to register it with
     * @param reload reads the files again, those of the players among them
     * @param config shows and sets the values of config.yml, with {@code reload} after a value is set
     */
    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            Runnable reload,
            ConfigCommand config) {
        SuggestionProvider<CommandSourceStack> suggestions = CoreSuggestions.of(args -> args.length >= 1
                ? config.complete(Arrays.copyOfRange(args, 1, args.length))
                : List.of());
        Command<CommandSourceStack> runConfig = context -> runLogged(new RunCommand() {
            @Override
            public int run() {
                String[] args = CommandInput.arguments(context);
                Reply reply = config.run(Arrays.copyOfRange(args, 1, args.length));
                // Brigadier only runs this with a key, and at most a value after it, which config always answers
                if (reply == null) reply = new Reply(Message.INCORRECT_ARGUMENT, true);
                if (reply.message() == Message.CONFIG_SET) resendPositions(context.getSource());
                send(context.getSource(), reply);
                return reply.error() ? 0 : 1;
            }

            @Override
            public CommandContext<CommandSourceStack> getContext() {
                return context;
            }
        });

        dispatcher.register(literal("curvebuilding")
                .requires(FabricCompat::isGameMaster)
                .then(literal("reload").executes(context -> {
                    reload.run();
                    resendPositions(context.getSource());
                    send(context.getSource(), new Reply(Message.RELOADED, false));
                    return 1;
                }))
                .then(literal("config").then(argument("key", word())
                        .suggests(suggestions)
                        .executes(runConfig)
                        .then(argument("value", word()).suggests(suggestions).executes(runConfig)))));
    }

    /** Tells every player's client which positions they have. */
    private static void resendPositions(CommandSourceStack source) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            sendPosPacket(player);
        }
    }

    private static void send(CommandSourceStack source, Reply reply) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSystemMessage(Component.literal(getMessage(reply.message(), reply.args())));
        } else if (reply.error()) {
            Chat.error(adaptPlayer(player), reply.message(), reply.args());
        } else {
            Chat.info(adaptPlayer(player), reply.message(), reply.args());
        }
    }
}
