package me.kous500.curvebuilding.bukkit.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.YamlConfig;
import org.bukkit.Color;

import static org.bukkit.Color.fromRGB;

public final class BukkitConfig extends Config {
    public final Color posColor;
    public final Color startColor;
    public final Color endColor;
    public final Color fColor;
    public final Color bColor;
    public final int posDensity;
    public final int lineDensity;
    public final int lineMaxLength;

    public BukkitConfig(YamlConfig config) {
        messageFilePath = config.getString("message-file");
        fineness = Math.max(1, config.getInteger("curve.fineness"));
        maxSetLength = Math.max(0, config.getInteger("curve.max-set-length"));
        maxPosValue = Math.max(1, config.getInteger("curve.max-pos-value"));
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = Math.max(-1, config.getInteger("curve.default-max-change-limit"));

        posColor = fromRGB(getIntInRange(config, "particles.pos.color.pos", 0x000000, 0xFFFFFF));
        startColor = fromRGB(getIntInRange(config, "particles.pos.color.start_pos", 0x000000, 0xFFFFFF));
        endColor = fromRGB(getIntInRange(config, "particles.pos.color.end_pos", 0x000000, 0xFFFFFF));
        fColor = fromRGB(getIntInRange(config, "particles.pos.color.f", 0x000000, 0xFFFFFF));
        bColor = fromRGB(getIntInRange(config, "particles.pos.color.b", 0x000000, 0xFFFFFF));

        posDensity = Math.max(1, config.getInteger("particles.pos.density"));
        lineDensity = Math.max(1, config.getInteger("particles.line.density"));
        lineMaxLength = Math.max(0, config.getInteger("particles.line.max-length"));
    }

    private int getIntInRange(YamlConfig config, String path, int min, int max) {
        int value = config.getInteger(path);
        return Math.max(min, Math.min(value, max));
    }
}