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
import me.kous500.curvebuilding.storage.PlayerStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

import static me.kous500.curvebuilding.CurveBuilding.loadResources;
import static me.kous500.curvebuilding.fabric.commands.Bc.setBcBuilder;
import static me.kous500.curvebuilding.fabric.commands.Pos.setPosBuilder;
import static me.kous500.curvebuilding.log.Log.setConsole;

/**
 * The mod on the server side, which is also there in single player: reads the files, registers the commands, keeps the
 * curves in the files of the players in the world folder and sends the positions to the clients.
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

    private static volatile @Nullable MinecraftServer runningServer;
    private static volatile @Nullable PlayerStore playerStore;

    @Override
    public void onInitialize() {
        setConsole(new FabricConsole());

        reloadFiles();

        // render-preview is read by each client from its own config.yml, so a server can only change curve
        ConfigFile configFile = CurveBuilding.configFile(DATA_FOLDER, jar(), FabricConfig::new);
        ConfigCommand configCommand = new ConfigCommand(configFile, List.of("curve"), CurveBuildingFabric::reload);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            setPosBuilder(dispatcher, () -> fabricConfig);
            setBcBuilder(dispatcher, () -> fabricConfig);
            CurveBuildingCommand.register(dispatcher, CurveBuildingFabric::reload, configCommand);
        });

        FabricCompat.registerClientboundPayload(PosDataPayload.ID, PosDataPayload.CODEC);

        PacketSender.start();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            runningServer = server;
            startPlayerStore(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerStore store = playerStore;
            if (store != null) store.load(handler.getPlayer().getUUID());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerStore store = playerStore;
            if (store != null) store.unload(handler.getPlayer().getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            closePlayerStore();
            runningServer = null;
        });
    }

    /**
     * The store of the curves of the players of the server that is running.
     *
     * @return the store, or null when no server is running
     */
    public static @Nullable PlayerStore playerStore() {
        return playerStore;
    }

    /**
     * Starts over: waits until the files of the players are written and stops keeping them, removes the curves of
     * every player, reads the config and the message file again, and keeps the curves in a new {@link PlayerStore}
     * with those of the players who are on read from their files. The clients are not sent the curves.
     */
    private static void reload() {
        closePlayerStore();
        PosManager.clearPosMap();
        reloadFiles();
        MinecraftServer server = runningServer;
        if (server == null) return;

        PlayerStore store = startPlayerStore(server);
        // The players who are on do not join again
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            store.load(player.getUUID());
        }
    }

    /** Keeps the curves in the world folder of {@code server}. */
    private static PlayerStore startPlayerStore(MinecraftServer server) {
        PlayerStore store =
                PlayerStore.start(server.getWorldPath(LevelResource.ROOT).resolve("curvebuilding").normalize());
        playerStore = store;
        return store;
    }

    private static void closePlayerStore() {
        PlayerStore store = playerStore;
        playerStore = null;
        if (store != null) store.close();
    }

    /** Reads the config and the message file again. */
    private static void reloadFiles() {
        fabricConfig = loadResources(DATA_FOLDER, jar(), FabricConfig::new);
    }

    private static ClassLoader jar() {
        return CurveBuildingFabric.class.getClassLoader();
    }
}
