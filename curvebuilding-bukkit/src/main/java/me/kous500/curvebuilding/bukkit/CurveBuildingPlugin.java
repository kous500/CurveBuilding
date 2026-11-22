package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import me.kous500.curvebuilding.CurveBuilding;
import me.kous500.curvebuilding.bukkit.commands.Bc;
import me.kous500.curvebuilding.bukkit.commands.CurveBuildingCommand;
import me.kous500.curvebuilding.bukkit.commands.Pos;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import me.kous500.curvebuilding.commands.config.ConfigCommand;
import me.kous500.curvebuilding.config.ConfigFile;
import me.kous500.curvebuilding.curve.PosManager;
import me.kous500.curvebuilding.log.Console;
import me.kous500.curvebuilding.storage.PlayerStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static me.kous500.curvebuilding.CurveBuilding.loadResources;
import static me.kous500.curvebuilding.log.Log.console;
import static me.kous500.curvebuilding.log.Log.setConsole;

/**
 * The Bukkit plugin: reads the files, registers the commands, keeps the curves in the files of the players and shows
 * them with particles.
 */
public final class CurveBuildingPlugin extends JavaPlugin {
    /**
     * The particles to show the curves with, or null when ParticleNativeAPI could not be loaded.
     */
    public static @Nullable ParticleList_1_13 particles_1_13;
    private static final Path DATA_FOLDER = Path.of("plugins/CurveBuilding");

    @SuppressWarnings("NotNullFieldNotInitialized") // onEnable reads it before it registers the commands
    private BukkitConfig config;
    private @Nullable PlayerStore playerStore;

    @Override
    public void onEnable() {
        setConsole(Console.of(getLogger()));

        // Before the files are read, which starts the particles when ParticleNativeAPI is loaded
        try {
            ParticleNativeAPI api = ParticleNativeCore.loadAPI(this);
            particles_1_13 = api.LIST_1_13;
        } catch (ParticleException e) {
            console().warning("Could not load ParticleNativeAPI, so the curves are not shown with particles", e);
        }

        getServer().getPluginManager().registerEvents(new PlayerFiles(() -> playerStore), this);
        reload();

        Bc bc = new Bc(() -> config);
        requireNonNull(this.getCommand("/bc")).setExecutor(bc);
        requireNonNull(this.getCommand("/bc")).setTabCompleter(bc);

        Pos pos = new Pos(() -> config);
        requireNonNull(this.getCommand("/bcpos")).setExecutor(pos);
        requireNonNull(this.getCommand("/bcpos")).setTabCompleter(pos);

        ConfigFile configFile = CurveBuilding.configFile(DATA_FOLDER, getClassLoader(), BukkitConfig::new);
        // The particles are sent by the server, so their settings can be changed too
        ConfigCommand configCommand = new ConfigCommand(configFile, List.of("curve", "particles"), this::reload);
        CurveBuildingCommand curveBuildingCommand = new CurveBuildingCommand(this::reload, configCommand);
        requireNonNull(this.getCommand("curvebuilding")).setExecutor(curveBuildingCommand);
        requireNonNull(this.getCommand("curvebuilding")).setTabCompleter(curveBuildingCommand);
    }

    /**
     * Starts over: stops the particles, waits until the files of the players are written and stops keeping them,
     * removes the curves of every player, reads the config and the message file again, keeps the curves in a new
     * {@link PlayerStore} with those of the players who are on read from their files, and starts the particles with
     * the new config.
     */
    private void reload() {
        SendParticles.stop();
        closePlayerStore();
        PosManager.clearPosMap();

        config = loadResources(DATA_FOLDER, getClassLoader(), BukkitConfig::new);

        PlayerStore store = PlayerStore.start(DATA_FOLDER);
        playerStore = store;
        // The players who are on do not join again
        for (Player player : getServer().getOnlinePlayers()) {
            store.load(player.getUniqueId());
        }

        SendParticles.start(this, config);
    }

    private void closePlayerStore() {
        if (playerStore != null) {
            playerStore.close();
            playerStore = null;
        }
    }

    @Override
    public void onDisable() {
        SendParticles.stop();
        closePlayerStore();
    }
}
