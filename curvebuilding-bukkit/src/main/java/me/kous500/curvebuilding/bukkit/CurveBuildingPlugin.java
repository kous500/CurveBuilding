package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import me.kous500.curvebuilding.Console;
import me.kous500.curvebuilding.bukkit.commands.Bc;
import me.kous500.curvebuilding.bukkit.commands.CurveBuildingCommand;
import me.kous500.curvebuilding.bukkit.commands.Pos;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import me.kous500.curvebuilding.math.PosManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static me.kous500.curvebuilding.CurveBuilding.*;

public final class CurveBuildingPlugin extends JavaPlugin {
    public static ParticleList_1_13 particles_1_13;
    private static final Path DATA_FOLDER = Path.of("plugins/CurveBuilding");

    @Override
    public void onEnable() {
        setConsole(Console.of(getLogger()));

        reloadConfigAndResources();

        try {
            ParticleNativeAPI api = ParticleNativeCore.loadAPI(this);
            particles_1_13 = api.LIST_1_13;
        } catch (ParticleException e) {
            console.warning("Could not load ParticleNativeAPI", e);
        }

        Bc bc = new Bc();
        requireNonNull(this.getCommand("/bc")).setExecutor(bc);
        requireNonNull(this.getCommand("/bc")).setTabCompleter(bc);

        Pos pos = new Pos();
        requireNonNull(this.getCommand("/bcpos")).setExecutor(pos);
        requireNonNull(this.getCommand("/bcpos")).setTabCompleter(pos);

        CurveBuildingCommand curveBuildingCommand = new CurveBuildingCommand(this);
        requireNonNull(this.getCommand("curvebuilding")).setExecutor(curveBuildingCommand);
        requireNonNull(this.getCommand("curvebuilding")).setTabCompleter(curveBuildingCommand);
    }

    public void reloadConfigAndResources() {
        SendParticles.stop();
        PosManager.clearPosMap();

        BukkitConfig bukkitConfig = loadResources(DATA_FOLDER, getClassLoader(), BukkitConfig::new);

        SendParticles.start(this, bukkitConfig);
    }

    @Override
    public void onDisable() {
        SendParticles.stop();
    }
}