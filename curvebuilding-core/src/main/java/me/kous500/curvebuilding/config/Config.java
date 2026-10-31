package me.kous500.curvebuilding.config;

public class Config {
    public String messageFilePath;

    public int fineness;
    public int maxSetLength;
    public int maxPosValue;
    public boolean tCenter;
    public int defaultMaxChangeLimit;

    public Config() {
    }

    /** Reads the settings that Bukkit and Fabric share from config.yml. */
    public Config(YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = Math.max(1, config.getInteger("curve.fineness"));
        maxSetLength = Math.max(0, config.getInteger("curve.max-set-length"));
        maxPosValue = Math.max(1, config.getInteger("curve.max-pos-value"));
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = Math.max(-1, config.getInteger("curve.default-max-change-limit"));
    }
}
