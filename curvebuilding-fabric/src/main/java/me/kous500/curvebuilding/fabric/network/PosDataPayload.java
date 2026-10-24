package me.kous500.curvebuilding.fabric.network;

import me.kous500.curvebuilding.math.Vector3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.NavigableMap;
import java.util.TreeMap;

public record PosDataPayload(NavigableMap<Integer, Vector3[]> sendPosData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PosDataPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("curvebuilding", "bezier-pos"));
    public static final StreamCodec<FriendlyByteBuf, PosDataPayload> CODEC = StreamCodec.of(PosDataPayload::encoder, PosDataPayload::decoder);

    @Override
    public Type<PosDataPayload> type() {
        return ID;
    }

    private static void encoder(FriendlyByteBuf buf, PosDataPayload payload) {
        buf.writeMap(
                payload.sendPosData,
                FriendlyByteBuf::writeInt,
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

    private static PosDataPayload decoder(FriendlyByteBuf posBuf) {
        return new PosDataPayload(posBuf.readMap(value -> new TreeMap<>(), FriendlyByteBuf::readInt, buf -> {
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
