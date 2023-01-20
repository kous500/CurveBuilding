package com.kous.curvebuilding;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.Particles_1_13;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import com.kous.curvebuilding.commands.pos.PosCommand;
import com.kous.curvebuilding.commands.bc.BcCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Timer;

public final class Main extends JavaPlugin {

    public static Particles_1_13 particles_1_13;

    @Override
    public void onEnable() {
        // Plugin startup logic

        try {
            ParticleNativeAPI api = ParticleNativeCore.loadAPI(this);
            particles_1_13 = api.getParticles_1_13();
        } catch (ParticleException e) {
            getLogger().info("An error occurred while loading ParticleNativeAPI: " + e);
        }

        Config config = new Config(this);

        BcCommand bcCommand = new BcCommand(this, config);
        this.getCommand("/bc").setExecutor(bcCommand);
        this.getCommand("/bc").setTabCompleter(bcCommand);

        PosCommand posCommand = new PosCommand(this);
        this.getCommand("/pos").setExecutor(posCommand);
        this.getCommand("/pos").setTabCompleter(posCommand);

        new Timer(true).schedule(new SendParticles(config), 0, 200);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
