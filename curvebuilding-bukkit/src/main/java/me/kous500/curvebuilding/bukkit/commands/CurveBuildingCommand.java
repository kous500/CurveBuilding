package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.commands.config.ConfigCommand;
import me.kous500.curvebuilding.message.Chat;
import me.kous500.curvebuilding.message.GameMessage;
import me.kous500.curvebuilding.message.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.message.Messages.getMessage;

/**
 * The command {@code /curvebuilding}, which reloads the files with {@code reload} and shows or sets a value of
 * config.yml with {@code config}.
 */
public class CurveBuildingCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("reload", "config");

    private final Runnable reload;
    private final ConfigCommand config;

    /**
     * The command, which runs {@code reload} for {@code /curvebuilding reload} and {@code config} for
     * {@code /curvebuilding config}.
     *
     * @param reload reads the files again
     * @param config shows and sets the values of config.yml
     */
    public CurveBuildingCommand(Runnable reload, ConfigCommand config) {
        this.reload = reload;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            reload.run();
            send(sender, new ConfigCommand.Reply(Message.RELOADED, false));
            return true;
        }
        ConfigCommand.@Nullable Reply reply = args.length >= 1 && args[0].equalsIgnoreCase("config")
                ? config.run(Arrays.copyOfRange(args, 1, args.length))
                : null;
        if (reply != null) {
            send(sender, reply);
            return true;
        }

        if (sender instanceof Player player) {
            Chat.error(adapt(player), GameMessage.INCORRECT_ARGUMENT);
        } else {
            sender.sendMessage(getMessage(Message.INCORRECT_ARGUMENT));
        }
        return false;
    }

    private static void send(CommandSender sender, ConfigCommand.Reply reply) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage(reply.message(), reply.args()));
        } else if (reply.error()) {
            Chat.error(adapt(player), reply.message(), reply.args());
        } else {
            Chat.info(adapt(player), reply.message(), reply.args());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("config")) {
            return config.complete(Arrays.copyOfRange(args, 1, args.length));
        }
        return Collections.emptyList();
    }
}
