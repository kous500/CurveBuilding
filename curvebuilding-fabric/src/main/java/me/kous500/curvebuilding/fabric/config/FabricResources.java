package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.Config;
import me.kous500.curvebuilding.Resources;
import me.kous500.curvebuilding.fabric.MainInitializer;
import me.kous500.curvebuilding.fabric.commands.ResourceType;

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
