package me.kous500.curvebuilding.fabric.network;

import com.sk89q.worldedit.fabric.FabricAdapter;
import me.kous500.curvebuilding.math.PosData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class PacketSender {
    private static Timer timer;

    public static void start() {
        if (timer != null) stop();
        timer = new Timer();
        timer.schedule(new PlayerListUpdateTask(), 0, 1000);
    }

    public static void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class PlayerListUpdateTask extends TimerTask {
        @Override
        public void run() {
            SendPacketMembers();
        }
    }

    private static final List<ServerPlayerEntity> playerList = new ArrayList<>();

    private static void SendPacketMembers() {
        for (ServerPlayerEntity player : playerList) {
            sendPosPacket(player);
        }
    }

    public static void sendPosPacket(ServerPlayerEntity player) {
        PosData posData = PosData.getPosMap().get(player.getUuid());
        if (posData == null) return;

        if (!playerList.contains(player)) playerList.add(player);

        if (posData.worldId != null && posData.worldId.equals(FabricAdapter.adapt(player.getWorld()).id())) {
            ServerPlayNetworking.send(player, new PosDataPayload(posData.p));
        } else {
            ServerPlayNetworking.send(player, new PosDataPayload(new TreeMap<>()));
        }
    }
}
