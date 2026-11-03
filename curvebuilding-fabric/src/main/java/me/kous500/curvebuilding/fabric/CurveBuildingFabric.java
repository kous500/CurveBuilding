package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import me.kous500.curvebuilding.fabric.config.FabricConfig;
import me.kous500.curvebuilding.fabric.network.PacketSender;
import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.nio.file.Path;

import static me.kous500.curvebuilding.CurveBuilding.loadResources;
import static me.kous500.curvebuilding.fabric.commands.Bc.setBcBuilder;
import static me.kous500.curvebuilding.fabric.commands.Pos.setPosBuilder;
import static me.kous500.curvebuilding.log.Log.setConsole;

/**
 * The mod on the server side, which is also there in single player: reads the files, registers the commands and sends
 * the positions to the clients.
 */
public class CurveBuildingFabric implements ModInitializer {
    /**
     * The folder of the config and the message files.
     */
    public static final Path DATA_FOLDER = Path.of("config/CurveBuilding");

    /**
     * The config read in {@link #onInitialize}, which runs before it is read.
     */
    @SuppressWarnings("NotNullFieldNotInitialized")
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
