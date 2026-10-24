package me.kous500.curvebuilding.fabric.network;

import me.kous500.curvebuilding.fabric.FabricCompat;
import me.kous500.curvebuilding.math.Vector3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public record PosDataPayload(NavigableMap<Integer, Vector3[]> sendPosData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PosDataPayload> ID = FabricCompat.payloadType("bezier-pos");
    public static final StreamCodec<FriendlyByteBuf, PosDataPayload> CODEC = StreamCodec.of(PosDataPayload::encoder, PosDataPayload::decoder);

    @Override
    public Type<PosDataPayload> type() {
        return ID;
    }

    // Same wire format as FriendlyByteBuf#writeMap, which newer Minecraft versions no longer have
    private static void encoder(FriendlyByteBuf buf, PosDataPayload payload) {
        buf.writeVarInt(payload.sendPosData.size());
        for (Map.Entry<Integer, Vector3[]> entry : payload.sendPosData.entrySet()) {
            buf.writeInt(entry.getKey());

            Vector3[] value = entry.getValue();
            for (int i : new int[]{0, 1, 2}) {
                if (value != null && value[i] != null) {
                    buf.writeBoolean(true);
                    buf.writeDouble(value[i].x());
                    buf.writeDouble(value[i].y());
                    buf.writeDouble(value[i].z());
                } else {
                    buf.writeBoolean(false);
                }
            }
        }
    }

    private static PosDataPayload decoder(FriendlyByteBuf buf) {
        NavigableMap<Integer, Vector3[]> posData = new TreeMap<>();
        int size = buf.readVarInt();
        for (int n = 0; n < size; n++) {
            int key = buf.readInt();

            Vector3[] vec = new Vector3[3];
            for (int i : new int[]{0, 1, 2}) {
                if (buf.readBoolean()) {
                    vec[i] = Vector3.at(buf.readDouble(), buf.readDouble(), buf.readDouble());
                }
            }
            posData.put(key, vec);
        }

        return new PosDataPayload(posData);
    }
}
