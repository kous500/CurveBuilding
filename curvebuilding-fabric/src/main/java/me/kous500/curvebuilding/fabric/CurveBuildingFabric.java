package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.CurveBuilding;
import me.kous500.curvebuilding.commands.config.ConfigCommand;
import me.kous500.curvebuilding.config.ConfigFile;
import me.kous500.curvebuilding.curve.PosManager;
import me.kous500.curvebuilding.fabric.commands.CurveBuildingCommand;
import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import me.kous500.curvebuilding.fabric.config.FabricConfig;
import me.kous500.curvebuilding.fabric.network.PacketSender;
import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.nio.file.Path;
import java.util.List;

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
     * The config read in {@link #onInitialize}, which runs before it is read, and read again by
     * {@code /curvebuilding}. The client reads it on its render thread.
     */
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static volatile FabricConfig fabricConfig;

    @Override
    public void onInitialize() {
        setConsole(new FabricConsole());

        reloadFiles();

        // render-preview is read by each client from its own config.yml, so a server can only change curve
        ConfigFile configFile = CurveBuilding.configFile(DATA_FOLDER, jar(), FabricConfig::new);
        ConfigCommand configCommand = new ConfigCommand(configFile, List.of("curve"), CurveBuildingFabric::reloadFiles);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            setPosBuilder(dispatcher, () -> fabricConfig);
            setBcBuilder(dispatcher, () -> fabricConfig);
            CurveBuildingCommand.register(dispatcher, CurveBuildingFabric::reloadFiles, configCommand);
        });

        FabricCompat.registerClientboundPayload(PosDataPayload.ID, PosDataPayload.CODEC);

        PacketSender.start();
    }

    /** Reads the config and the message file again, and removes the positions of every player. */
    private static void reloadFiles() {
        PosManager.clearPosMap();
        fabricConfig = loadResources(DATA_FOLDER, jar(), FabricConfig::new);
    }

    private static ClassLoader jar() {
        return CurveBuildingFabric.class.getClassLoader();
    }
}
