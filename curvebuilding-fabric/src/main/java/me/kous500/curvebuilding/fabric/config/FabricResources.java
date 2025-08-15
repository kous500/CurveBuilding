package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Resources;
import me.kous500.curvebuilding.config.YamlConfig;

import static me.kous500.curvebuilding.Util.messageReplace;

public class FabricResources implements Resources {
    private final Config config;
    private final YamlConfig messages;

    public FabricResources(Config config, YamlConfig messages) {
        this.config = config;
        this.messages = messages;
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