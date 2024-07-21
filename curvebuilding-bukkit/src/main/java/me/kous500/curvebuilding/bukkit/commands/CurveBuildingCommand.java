package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.bukkit.CurveBuildingPlugin;
import me.kous500.curvebuilding.bukkit.SendParticles;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import me.kous500.curvebuilding.bukkit.config.BukkitResources;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.kous500.curvebuilding.CurveBuilding.*;

public class CurveBuildingCommand implements TabExecutor {
    private final CurveBuildingPlugin plugin;

    public CurveBuildingCommand(CurveBuildingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equals("reload")) {
            reload();
            String message = "Reloaded curvebuilding.";
            if (sender instanceof Player) {
                sender.sendMessage(message);
            } else {
                plugin.getLogger().info(message);
            }
            return true;
        } else {
            if (sender instanceof Player) {
                sender.sendMessage("§c" + getMessage("messages.incorrect-argument"));
            } else {
                plugin.getLogger().info(getMessage("messages.incorrect-argument"));
            }
            return false;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("curvebuilding")) {
            for (int i = 0; i < args.length; i++) {
                completions = new ArrayList<>();
                List<String> commands = new ArrayList<>();

                if (i == 0) {
                    commands.add("reload");
                }

                StringUtil.copyPartialMatches(args[i], commands, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }

    private void reload() {
        SendParticles.stop();
        PosData.getPosMap().clear();
        setResources(new BukkitResources(plugin));
        SendParticles.start((BukkitConfig) config);
    }
}
