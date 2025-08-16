package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import me.kous500.curvebuilding.MainInitializer;
import me.kous500.curvebuilding.bukkit.commands.Bc;
import me.kous500.curvebuilding.bukkit.commands.CurveBuildingCommand;
import me.kous500.curvebuilding.bukkit.commands.Pos;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import me.kous500.curvebuilding.bukkit.config.BukkitResources;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.ResourceType;
import me.kous500.curvebuilding.config.YamlConfig;
import me.kous500.curvebuilding.math.PosData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import static java.util.Objects.requireNonNull;
import static me.kous500.curvebuilding.CurveBuilding.*;

public final class CurveBuildingPlugin extends JavaPlugin implements MainInitializer {
    public static ParticleList_1_13 particles_1_13;
    private static final String PLUGIN_CONFIG_PASS = "plugins/CurveBuilding";

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit")){
            fawe = true;
        }

        reloadConfigAndResources();
        setConsole((new BukkitConsole(this)));

        try {
            ParticleNativeAPI api = ParticleNativeCore.loadAPI(this);
            particles_1_13 = api.LIST_1_13;
        } catch (ParticleException e) {
            getLogger().warning(getMessage("messages.plugin-load-error", "ParticleNativeAPI") + e);
        }

        Bc bc = new Bc(this);
        requireNonNull(this.getCommand("/bc")).setExecutor(bc);
        requireNonNull(this.getCommand("/bc")).setTabCompleter(bc);

        Pos pos = new Pos(this);
        requireNonNull(this.getCommand("/bcpos")).setExecutor(pos);
        requireNonNull(this.getCommand("/bcpos")).setTabCompleter(pos);

        CurveBuildingCommand curveBuildingCommand = new CurveBuildingCommand(this);
        requireNonNull(this.getCommand("curvebuilding")).setExecutor(curveBuildingCommand);
        requireNonNull(this.getCommand("curvebuilding")).setTabCompleter(curveBuildingCommand);
    }

    public void reloadConfigAndResources() {
        SendParticles.stop();
        PosData.getPosMap().clear();

        ResourceFiles.setup(this);

        File configFile = new File(this.getConfigPass(), "config.yml");
        YamlConfig yamlConfig = YamlConfig.loadConfiguration(configFile, ResourceType.config, this);

        BukkitConfig bukkitConfig = new BukkitConfig(yamlConfig);

        String messageFileName = bukkitConfig.messageFilePath.replace("%datafolder%", this.getConfigPass());
        File messageFile = new File(messageFileName);
        YamlConfig messagesConfig = YamlConfig.loadConfiguration(messageFile, ResourceType.message, this);

        setResources(new BukkitResources(bukkitConfig, messagesConfig));

        SendParticles.start(bukkitConfig);
    }

    @Override
    public void onDisable() {
        SendParticles.stop();
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public String getConfigPass() {
        return PLUGIN_CONFIG_PASS;
    }
}