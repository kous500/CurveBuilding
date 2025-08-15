package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.Resources;
import me.kous500.curvebuilding.config.YamlConfig;
import me.kous500.curvebuilding.MainInitializer;
import me.kous500.curvebuilding.config.ResourceType;

import static me.kous500.curvebuilding.Util.messageReplace;

public class FabricResources implements Resources {

    public FabricResources(MainInitializer mainInitializer) {
        ResourceFiles resourceFiles = ResourceFiles.load(mainInitializer);

        config = new FabricConfig(resourceFiles.get("%datafolder%/config.yml", ResourceType.config));
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

    private static Config config;

    private static YamlConfig messages;
}
