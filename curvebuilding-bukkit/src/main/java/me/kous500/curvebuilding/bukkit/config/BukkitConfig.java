package me.kous500.curvebuilding.bukkit.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.YamlConfig;
import org.bukkit.Color;

import static org.bukkit.Color.fromRGB;

/**
 * config.ymlのデータを読み込む。
 */
public final class BukkitConfig extends Config {
    public Color posColor;
    public Color startColor;
    public Color endColor;
    public Color fColor;
    public Color bColor;

    public int posDensity;
    public int lineDensity;
    public int lineMaxLength;

    public BukkitConfig(YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = config.getInteger("curve.fineness", 1);
        maxSetLength = config.getInteger("curve.max-set-length", 0);
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = config.getInteger("curve.default-max-change-limit", 0);

        posColor = fromRGB(config.getInteger("particles.pos.color.pos", 0x000000, 0xFFFFFF));
        startColor = fromRGB(config.getInteger("particles.pos.color.start_pos", 0x000000, 0xFFFFFF));
        endColor = fromRGB(config.getInteger("particles.pos.color.end_pos", 0x000000, 0xFFFFFF));
        fColor = fromRGB(config.getInteger("particles.pos.color.f", 0x000000, 0xFFFFFF));
        bColor = fromRGB(config.getInteger("particles.pos.color.b", 0x000000, 0xFFFFFF));

        posDensity = config.getInteger("particles.pos.density", 1);
        lineDensity = config.getInteger("particles.line.density", 1);
        lineMaxLength = config.getInteger("particles.line.max-length", 0);
    }
}