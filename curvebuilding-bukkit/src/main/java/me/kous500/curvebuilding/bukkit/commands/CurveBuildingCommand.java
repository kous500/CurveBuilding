package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.bukkit.CurveBuildingPlugin;
import me.kous500.curvebuilding.config.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;

public class CurveBuildingCommand implements TabExecutor {
    private final CurveBuildingPlugin plugin;

    public CurveBuildingCommand(CurveBuildingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfigAndResources();
            if (sender instanceof Player player) {
                Chat.info(adapt(player), Message.RELOADED);
            } else {
                sender.sendMessage(getMessage(Message.RELOADED));
            }
            return true;
        } else {
            if (sender instanceof Player player) {
                Chat.error(adapt(player), GameMessage.INCORRECT_ARGUMENT);
            } else {
                sender.sendMessage(getMessage(Message.INCORRECT_ARGUMENT));
            }
            return false;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Collections.singletonList("reload"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}