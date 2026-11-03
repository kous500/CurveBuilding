package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.commands.pos.PosCompletion;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.message.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Supplier;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;
import static me.kous500.curvebuilding.message.Messages.getMessage;

/**
 * The command {@code //bcpos} on Bukkit.
 */
public class Pos implements TabExecutor {
    private final Supplier<Config> config;

    /**
     * The command, which reads the config in use from {@code config} each time it runs.
     *
     * @param config gives the config in use
     */
    public Pos(Supplier<Config> config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // An error was already shown, so Bukkit should not add the usage from plugin.yml
        if (sender instanceof Player player) {
            posCommand(adapt(player), args, config.get());
        } else {
            sender.sendMessage(getMessage(Message.NON_PLAYER_EXECUTION));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("/bcpos")) return List.of();
        return PosCompletion.complete(args);
    }
}
