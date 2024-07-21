package me.kous500.curvebuilding.bukkit;

import me.kous500.curvebuilding.Console;
import me.kous500.curvebuilding.CurveBuilding;

public class BukkitConsole implements Console {
    private final CurveBuildingPlugin plugin;

    BukkitConsole(CurveBuildingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }
}
