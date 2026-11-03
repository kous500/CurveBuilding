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
import me.kous500.curvebuilding.curve.PosManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static me.kous500.curvebuilding.CurveBuilding.console;
import static me.kous500.curvebuilding.CurveBuilding.loadResources;
import static me.kous500.curvebuilding.CurveBuilding.setConsole;

/**
 * The Bukkit plugin: reads the files, registers the commands and shows the curves with particles.
 */
public final class CurveBuildingPlugin extends JavaPlugin {
    /**
     * The particles to show the curves with, or null when ParticleNativeAPI could not be loaded.
     */
    public static @Nullable ParticleList_1_13 particles_1_13;
    private static final Path DATA_FOLDER = Path.of("plugins/CurveBuilding");

    @Override
    public void onEnable() {
        setConsole(Console.of(getLogger()));

        // Before the files are read, which starts the particles when ParticleNativeAPI is loaded
        try {
            ParticleNativeAPI api = ParticleNativeCore.loadAPI(this);
            particles_1_13 = api.LIST_1_13;
        } catch (ParticleException e) {
            console.warning("Could not load ParticleNativeAPI, so the curves are not shown with particles", e);
        }

        reloadConfigAndResources();

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

    /**
     * Reads the config and the message file again, and restarts the particles with the new config. The curves of every
     * player are removed.
     */
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
