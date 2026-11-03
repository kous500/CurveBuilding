package me.kous500.curvebuilding.config;

import org.jspecify.annotations.Nullable;

/** The settings of config.yml that every platform has. The platforms add their own in a subclass. */
public class Config {
    /** The path of the message file, with {@code %datafolder%} for the data folder, or null without a setting. */
    public @Nullable String messageFilePath;

    /** How many steps {@code //bc} takes along the curve per block, at least 1. */
    public int fineness;
    /** The longest curve that {@code //bc} builds, in blocks. */
    public int maxSetLength;
    /** The highest point number of {@code //bcpos}, at least 1. */
    public int maxPosValue;
    /** The most blocks one {@code //bc} may change, or -1 for no limit beyond the one of the player in WorldEdit. */
    public int defaultMaxChangeLimit;

    public Config() {
    }

    /**
     * Reads the settings that every platform shares from config.yml.
     *
     * @param config the values of config.yml
     */
    public Config(YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = Math.max(1, config.getInteger("curve.fineness"));
        maxSetLength = Math.max(0, config.getInteger("curve.max-set-length"));
        maxPosValue = Math.max(1, config.getInteger("curve.max-pos-value"));
        defaultMaxChangeLimit = Math.max(-1, config.getInteger("curve.default-max-change-limit"));
    }
}
