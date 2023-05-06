package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.fabric.config.FabricConfig;
import me.kous500.curvebuilding.fabric.config.FabricResources;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static me.kous500.curvebuilding.CurveBuilding.config;
import static me.kous500.curvebuilding.CurveBuilding.setResources;
import static me.kous500.curvebuilding.fabric.commands.Bc.setBcBuilder;
import static me.kous500.curvebuilding.fabric.commands.Pos.setPosBuilder;

public class FabricCurveBuilding implements ModInitializer, MainInitializer {
    public static boolean debugMode = true;
    public static final String MOD_DATA_FOLDER = "config/CurveBuilding";

    public static FabricConfig fabricConfig;

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        setResources(new FabricResources(this));
        fabricConfig = (FabricConfig) config;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            setPosBuilder(dispatcher);
            setBcBuilder(dispatcher);
        });

        SendPacketTimer.start();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public String getDateFolder() {
        return MOD_DATA_FOLDER;
    }
}
