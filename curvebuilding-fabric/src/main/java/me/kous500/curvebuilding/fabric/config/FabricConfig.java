package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.Config;

public class FabricConfig extends Config {
    public FabricConfig (YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = config.getInteger("curve.fineness", 1);
        maxSetLength = config.getInteger("curve.max-set-length", 0);
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = config.getInteger("curve.default-max-change-limit", 0);
    }
}
