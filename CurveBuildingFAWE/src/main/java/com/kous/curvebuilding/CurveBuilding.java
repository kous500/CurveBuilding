package com.kous.curvebuilding;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import com.kous.curvebuilding.commands.bc.BcCommand;
import com.kous.curvebuilding.commands.pos.PosCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Timer;

import static com.kous.curvebuilding.Message.getMessage;

public final class CurveBuilding extends JavaPlugin {

    public static Config config;
    public static ParticleList_1_13 particles_1_13;

    @Override
    public void onEnable() {
        // Plugin startup logic

        config = new Config(this);

        new Message(this).load();

        try {
            ParticleNativeAPI api = ParticleNativeCore.loadAPI(this);
            particles_1_13 = api.LIST_1_13;
        } catch (ParticleException e) {
            getLogger().warning(getMessage("messages.plugin-load-error", "ParticleNativeAPI") + e);
        }

        BcCommand bcCommand = new BcCommand(this);
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
