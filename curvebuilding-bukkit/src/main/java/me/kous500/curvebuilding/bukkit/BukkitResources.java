package me.kous500.curvebuilding.bukkit;

import me.kous500.curvebuilding.Config;
import me.kous500.curvebuilding.Resources;
import org.bukkit.configuration.file.YamlConfiguration;

import static me.kous500.curvebuilding.Util.messageReplace;

public class BukkitResources implements Resources {
    BukkitResources(CurveBuildingPlugin plugin) {
        config = new BukkitConfig(plugin);
    }
    @Override
    public String getMessage(String path, Object... args) {
        String message = messages.getString(path, path);
        return messageReplace(message, args);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    static YamlConfiguration messages;
    private static Config config;
}