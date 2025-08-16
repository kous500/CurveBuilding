package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.MainInitializer;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.ResourceType;
import me.kous500.curvebuilding.config.YamlConfig;
import me.kous500.curvebuilding.fabric.config.FabricConfig;
import me.kous500.curvebuilding.fabric.config.FabricResources;
import me.kous500.curvebuilding.fabric.network.PacketSender;
import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.io.File;

import static me.kous500.curvebuilding.CurveBuilding.setResources;
import static me.kous500.curvebuilding.fabric.commands.Bc.setBcBuilder;
import static me.kous500.curvebuilding.fabric.commands.Pos.setPosBuilder;

public class CurveBuildingFabric implements ModInitializer, MainInitializer {
    public static boolean debugMode = true;
    public static final String MOD_CONFIG_PASS = "config/CurveBuilding";

    public static FabricConfig fabricConfig;

    @Override
    public void onInitialize() {
        ResourceFiles.setup(this);

        File configFile = new File(this.getConfigPass(), "config.yml");
        YamlConfig yamlConfig = YamlConfig.loadConfiguration(configFile, ResourceType.config, this);

        FabricConfig loadedConfig = new FabricConfig(yamlConfig);
        fabricConfig = loadedConfig;

        String messageFileName = loadedConfig.messageFilePath.replace("%datafolder%", this.getConfigPass());
        File messageFile = new File(messageFileName);
        YamlConfig messagesConfig = YamlConfig.loadConfiguration(messageFile, ResourceType.message, this);

        FabricResources resources = new FabricResources(loadedConfig, messagesConfig);
        setResources(resources);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            setPosBuilder(dispatcher);
            setBcBuilder(dispatcher);
        });

        PayloadTypeRegistry.playS2C().register(PosDataPayload.ID, PosDataPayload.CODEC);

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