package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.bukkit.CurveBuildingPlugin;
import me.kous500.curvebuilding.commands.pos.PosCompletion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;

public class Pos implements TabExecutor {
    private final CurveBuildingPlugin plugin;

    public Pos(CurveBuildingPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            return posCommand(adapt((Player) sender), command.getName(), args);
        } else {
            plugin.getLogger().info(getMessage("messages.non-player-execution"));
            return true;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("/bcpos")) return List.of();
        return PosCompletion.complete(args);
    }
}