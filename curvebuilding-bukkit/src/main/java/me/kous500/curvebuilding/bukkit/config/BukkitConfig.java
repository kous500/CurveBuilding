package me.kous500.curvebuilding.bukkit.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.YamlConfig;
import org.bukkit.Color;

import static org.bukkit.Color.fromRGB;

/**
 * The settings of config.yml for the particles on Bukkit, with the shared ones.
 */
public final class BukkitConfig extends Config {
    public final Color posColor;
    public final Color startColor;
    public final Color endColor;
    public final Color fColor;
    public final Color bColor;
    public final int posDensity;
    public final int lineDensity;
    public final int lineMaxLength;

    /**
     * Reads the settings from config.yml.
     *
     * @param config the values of config.yml
     */
    public BukkitConfig(YamlConfig config) {
        super(config);

        posColor = fromRGB(getRgb(config, "particles.pos.color.pos"));
        startColor = fromRGB(getRgb(config, "particles.pos.color.start_pos"));
        endColor = fromRGB(getRgb(config, "particles.pos.color.end_pos"));
        fColor = fromRGB(getRgb(config, "particles.pos.color.f"));
        bColor = fromRGB(getRgb(config, "particles.pos.color.b"));

        posDensity = Math.max(1, config.getInteger("particles.pos.density"));
        lineDensity = Math.max(1, config.getInteger("particles.line.density"));
        lineMaxLength = Math.max(0, config.getInteger("particles.line.max-length"));
    }

    private static int getRgb(YamlConfig config, String path) {
        return Math.clamp(config.getInteger(path), 0x000000, 0xFFFFFF);
    }
}
