package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.MainInitializer;
import me.kous500.curvebuilding.fabric.config.FabricConfig;
import me.kous500.curvebuilding.fabric.network.PacketSender;
import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static me.kous500.curvebuilding.CurveBuilding.loadResources;
import static me.kous500.curvebuilding.fabric.commands.Bc.setBcBuilder;
import static me.kous500.curvebuilding.fabric.commands.Pos.setPosBuilder;

public class CurveBuildingFabric implements ModInitializer, MainInitializer {
    public static boolean debugMode = true;
    public static final String MOD_CONFIG_PASS = "config/CurveBuilding";

    public static FabricConfig fabricConfig;

    @Override
    public void onInitialize() {
        fabricConfig = loadResources(this, FabricConfig::new);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            setPosBuilder(dispatcher);
            setBcBuilder(dispatcher);
        });

        FabricCompat.registerClientboundPayload(PosDataPayload.ID, PosDataPayload.CODEC);

        PacketSender.start();
    }

    @Override
    public ClassLoader getMainClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public String getConfigPass() {
        return MOD_CONFIG_PASS;
    }
}