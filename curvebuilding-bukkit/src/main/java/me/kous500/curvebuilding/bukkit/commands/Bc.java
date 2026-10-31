package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.bukkit.CurveBuildingPlugin;
import me.kous500.curvebuilding.commands.bc.BcCommand;
import me.kous500.curvebuilding.commands.bc.BcCompletion;
import me.kous500.curvebuilding.commands.bc.BcEdit;
import me.kous500.curvebuilding.config.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;

public class Bc implements TabExecutor {
    private final CurveBuildingPlugin plugin;


    public Bc(CurveBuildingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("/bc")) {
            if (sender instanceof Player) {
                BcCommand argument = new BcCommand(args, adapt((Player) sender));
                if (argument.success) {
                    new BcEdit(adapt((Player) sender), argument);
                    return true;
                }
            } else {
                plugin.getLogger().info(getMessage(Message.NON_PLAYER_EXECUTION));
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return BcCompletion.complete(args);
    }
}