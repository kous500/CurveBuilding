package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.fabric.config.FabricConfig;
import me.kous500.curvebuilding.fabric.network.PacketSender;
import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.nio.file.Path;

import static me.kous500.curvebuilding.CurveBuilding.loadResources;
import static me.kous500.curvebuilding.CurveBuilding.setConsole;
import static me.kous500.curvebuilding.fabric.commands.Bc.setBcBuilder;
import static me.kous500.curvebuilding.fabric.commands.Pos.setPosBuilder;

public class CurveBuildingFabric implements ModInitializer {
    public static final Path DATA_FOLDER = Path.of("config/CurveBuilding");

    public static FabricConfig fabricConfig;

    @Override
    public void onInitialize() {
        setConsole(new FabricConsole());

        fabricConfig = loadResources(DATA_FOLDER, getClass().getClassLoader(), FabricConfig::new);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            setPosBuilder(dispatcher);
            setBcBuilder(dispatcher);
        });

        FabricCompat.registerClientboundPayload(PosDataPayload.ID, PosDataPayload.CODEC);

        PacketSender.start();
    }
}