package me.kous500.curvebuilding.fabric.network;

import me.kous500.curvebuilding.fabric.FabricCompat;
import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.math.PosManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Sends the curve positions of each player to their client, which draws the preview from them.
 */
public class PacketSender {
    private static final int RESEND_INTERVAL_TICKS = 20;
    private static boolean started;

    /** Resends the positions of the players who have any once a second, on the server thread. */
    public static void start() {
        if (started) return;
        started = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % RESEND_INTERVAL_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (PosManager.getPosMap().containsKey(player.getUUID())) {
                    sendPosPacket(player);
                }
            }
        });
    }

    /**
     * Sends the positions of {@code player} in their current world. With none, it sends no positions, so that the client
     * stops drawing the ones it has, for example after all of them were cleared.
     */
    public static void sendPosPacket(ServerPlayer player) {
        PosData posData = PosManager.getPosMap().get(player.getUUID());
        if (posData != null && posData.isIn(FabricCompat.adaptWorld(player.level()))) {
            // Never changed, so it can still be written out after the next command changes the positions
            ServerPlayNetworking.send(player, new PosDataPayload(posData.points()));
        } else {
            ServerPlayNetworking.send(player, new PosDataPayload(new TreeMap<>()));
        }
    }
}
