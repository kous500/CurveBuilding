package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.commands.bc.BcCompletion;
import me.kous500.curvebuilding.config.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.commands.bc.BcCommand.bcCommand;

/**
 * The command {@code //bc} on Bukkit.
 */
public class Bc implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // An error was already shown, so Bukkit should not add the usage from plugin.yml
        if (sender instanceof Player player) {
            bcCommand(adapt(player), args);
        } else {
            sender.sendMessage(getMessage(Message.NON_PLAYER_EXECUTION));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return BcCompletion.complete(args);
    }
}
