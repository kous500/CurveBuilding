package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import me.kous500.curvebuilding.bukkit.commands.Bc;
import me.kous500.curvebuilding.bukkit.commands.Pos;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Timer;

import static java.util.Objects.requireNonNull;
import static me.kous500.curvebuilding.CurveBuilding.*;


public final class CurveBuildingPlugin extends JavaPlugin {
    public static ParticleList_1_13 particles_1_13;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit")){
            fawe = true;
        }

        setResources(new BukkitResources(this));

        new Message(this).load();

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
        requireNonNull(this.getCommand("/pos")).setExecutor(pos);
        requireNonNull(this.getCommand("/pos")).setTabCompleter(pos);

        new Timer(true).schedule(new SendParticles((BukkitConfig) config), 0, 200);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
