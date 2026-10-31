package me.kous500.curvebuilding.fabric.network;

import me.kous500.curvebuilding.fabric.FabricCompat;
import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public record PosDataPayload(NavigableMap<Integer, CurvePoint> sendPosData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PosDataPayload> ID = FabricCompat.payloadType("bezier-pos");
    public static final StreamCodec<FriendlyByteBuf, PosDataPayload> CODEC = StreamCodec.of(PosDataPayload::encoder, PosDataPayload::decoder);

    @Override
    public Type<PosDataPayload> type() {
        return ID;
    }

    // Same wire format as FriendlyByteBuf#writeMap, which newer Minecraft versions no longer have
    private static void encoder(FriendlyByteBuf buf, PosDataPayload payload) {
        buf.writeVarInt(payload.sendPosData.size());
        for (Map.Entry<Integer, CurvePoint> entry : payload.sendPosData.entrySet()) {
            buf.writeInt(entry.getKey());

            for (CurvePoint.Part part : CurvePoint.Part.values()) {
                Vector3 position = entry.getValue().get(part);
                if (position != null) {
                    buf.writeBoolean(true);
                    buf.writeDouble(position.x());
                    buf.writeDouble(position.y());
                    buf.writeDouble(position.z());
                } else {
                    buf.writeBoolean(false);
                }
            }
        }
    }

    private static PosDataPayload decoder(FriendlyByteBuf buf) {
        NavigableMap<Integer, CurvePoint> posData = new TreeMap<>();
        int size = buf.readVarInt();
        for (int n = 0; n < size; n++) {
            int key = buf.readInt();

            CurvePoint point = CurvePoint.EMPTY;
            for (CurvePoint.Part part : CurvePoint.Part.values()) {
                if (buf.readBoolean()) {
                    point = point.with(part, Vector3.at(buf.readDouble(), buf.readDouble(), buf.readDouble()));
                }
            }
            posData.put(key, point);
        }

        return new PosDataPayload(posData);
    }
}
