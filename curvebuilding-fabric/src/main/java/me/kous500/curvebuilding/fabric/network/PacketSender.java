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

    /** Resends the positions once a second, on the server thread. */
    public static void start() {
        if (started) return;
        started = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % RESEND_INTERVAL_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendPosPacket(player);
            }
        });
    }

    public static void sendPosPacket(ServerPlayer player) {
        PosData posData = PosManager.getPosMap().get(player.getUUID());
        if (posData == null) return;

        if (posData.getWorldId() != null && posData.getWorldId().equals(FabricCompat.adaptWorld(player.level()).id())) {
            ServerPlayNetworking.send(player, new PosDataPayload(posData.getMap()));
        } else {
            ServerPlayNetworking.send(player, new PosDataPayload(new TreeMap<>()));
        }
    }
}
