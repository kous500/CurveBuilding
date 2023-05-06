package me.kous500.curvebuilding.fabric;

import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.Vector3;
import io.netty.buffer.Unpooled;
import me.kous500.curvebuilding.PosData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;

public class PosDataPacket {
    public static final Identifier PACKET_ID = new Identifier("curvebuilding", "pos-data");
    private static final List<ServerPlayerEntity> playerList = new ArrayList<>();

    public static void SendPacketMembers() {
        for (ServerPlayerEntity player : playerList) {
            sendPosPacket(player);
        }
    }

    public static void sendPosPacket(ServerPlayerEntity player) {
        PosData posData = PosData.getPosMap().get(player.getUuid());
        if (posData == null) return;

        if (!playerList.contains(player)) playerList.add(player);
        NavigableMap<Integer, Vector3[]> sendPosData = new TreeMap<>();
        if (posData.worldId != null && posData.worldId.equals(FabricAdapter.adapt(player.world).getId())) sendPosData = posData.p;
        PacketByteBuf posBuf = new PacketByteBuf(Unpooled.buffer());

        posBuf.writeMap(
                sendPosData,
                PacketByteBuf::writeInt,
                (buf, value) -> {
                    if (value == null) return;

                    for (int i : new int[]{0, 1, 2}) {
                        if (value[i] != null) {
                            buf.writeBoolean(true);
                            buf.writeDouble(value[i].getX());
                            buf.writeDouble(value[i].getY());
                            buf.writeDouble(value[i].getZ());
                        } else {
                            buf.writeBoolean(false);
                        }
                    }
                }
        );

        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(PACKET_ID, posBuf));
    }

    public static NavigableMap<Integer, Vector3[]> readPosPacket(PacketByteBuf posBuf) {
        return posBuf.readMap(value -> new TreeMap<>(), PacketByteBuf::readInt, buf -> {
            Vector3[] vec = new Vector3[3];
            for (int i : new int[]{0, 1, 2}) {
                if (buf.readBoolean()) {
                    vec[i] = Vector3.at(buf.readDouble(), buf.readDouble(), buf.readDouble());
                }
            }

            return vec;
        });
    }
}
