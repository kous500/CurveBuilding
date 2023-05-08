package me.kous500.curvebuilding.bukkit.config;

import me.kous500.curvebuilding.MainInitializer;
import me.kous500.curvebuilding.config.*;

import static me.kous500.curvebuilding.Util.messageReplace;

public class BukkitResources implements Resources {
    static YamlConfig messages;
    private static Config config;

    public BukkitResources(MainInitializer mainInitializer) {
        ResourceFiles resourceFiles = ResourceFiles.load(mainInitializer);
        config = new BukkitConfig(resourceFiles.get("%datafolder%/config.yml", ResourceType.config));
        messages = resourceFiles.get(config.messageFilePath, ResourceType.message);
    }

    @Override
    public String getMessage(String path, Object... args) {
        String message = messages.getString(path);
        return messageReplace(message, args);
    }

    @Override
    public Config getConfig() {
        return config;
    }
}
