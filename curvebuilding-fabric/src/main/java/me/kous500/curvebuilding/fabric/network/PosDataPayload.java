package me.kous500.curvebuilding.fabric.network;

import me.kous500.curvebuilding.math.Vector3;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.NavigableMap;
import java.util.TreeMap;

public record PosDataPayload(NavigableMap<Integer, Vector3[]> sendPosData) implements CustomPayload {
    public static final CustomPayload.Id<PosDataPayload> ID = new CustomPayload.Id<>(Identifier.of("curvebuilding", "bezier-pos"));
    public static final PacketCodec<PacketByteBuf, PosDataPayload> CODEC = PacketCodec.ofStatic(
            PosDataPayload::encoder,
            PosDataPayload::decoder
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void encoder(PacketByteBuf buf, PosDataPayload payload) {
        buf.writeMap(
                payload.sendPosData,
                PacketByteBuf::writeInt,
                (mapBuf, value) -> {
                    if (value == null) return;

                    for (int i : new int[]{0, 1, 2}) {
                        if (value[i] != null) {
                            mapBuf.writeBoolean(true);
                            mapBuf.writeDouble(value[i].x());
                            mapBuf.writeDouble(value[i].y());
                            mapBuf.writeDouble(value[i].z());
                        } else {
                            mapBuf.writeBoolean(false);
                        }
                    }
                }
        );
    }

    private static PosDataPayload decoder(PacketByteBuf posBuf) {
        return new PosDataPayload(posBuf.readMap(value -> new TreeMap<>(), PacketByteBuf::readInt, buf -> {
            Vector3[] vec = new Vector3[3];
            for (int i : new int[]{0, 1, 2}) {
                if (buf.readBoolean()) {
                    vec[i] = Vector3.at(buf.readDouble(), buf.readDouble(), buf.readDouble());
                }
            }

            return vec;
        }));
    }
}
