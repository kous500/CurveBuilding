package me.kous500.curvebuilding.fabric.network;

import io.netty.buffer.ByteBuf;
import me.kous500.curvebuilding.curve.CurvePoint;
import me.kous500.curvebuilding.fabric.compat.FabricCompat;
import me.kous500.curvebuilding.math.Vector3;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
*///?}

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The positions of a player, sent to their client for the preview.
 *
 * @param sendPosData the points by their number
 */
//? if >=1.20.5 {
public record PosDataPayload(NavigableMap<Integer, CurvePoint> sendPosData) implements CustomPacketPayload {
    /**
     * The type of this payload.
     */
    public static final CustomPacketPayload.Type<PosDataPayload> ID = FabricCompat.payloadType("bezier-pos");
    /**
     * Writes and reads this payload.
     */
    public static final StreamCodec<FriendlyByteBuf, PosDataPayload> CODEC =
            StreamCodec.of(PosDataPayload::encoder, PosDataPayload::decoder);

    @Override
    public Type<PosDataPayload> type() {
        return ID;
    }
//?} else {
/*public record PosDataPayload(NavigableMap<Integer, CurvePoint> sendPosData) implements FabricPacket {
    /^*
     * The type of this packet. Before 1.20.5, it both writes and reads it.
     ^/
    public static final PacketType<PosDataPayload> ID =
            PacketType.create(FabricCompat.packetId("bezier-pos"), PosDataPayload::decoder);

    @Override
    public void write(FriendlyByteBuf buf) {
        encoder(buf, this);
    }

    @Override
    public PacketType<?> getType() {
        return ID;
    }
*///?}

    // Same wire format as FriendlyByteBuf#writeMap, which Minecraft 26.3 removed
    private static void encoder(FriendlyByteBuf buf, PosDataPayload payload) {
        buf.writeVarInt(payload.sendPosData.size());
        for (Map.Entry<Integer, CurvePoint> entry : payload.sendPosData.entrySet()) {
            writePoint(buf, entry.getKey(), entry.getValue());
        }
    }

    // Takes a ByteBuf: FriendlyByteBuf overrides these methods from 1.20.2, and 1.20 - 1.20.1 do not have the overrides
    private static void writePoint(ByteBuf buf, int n, CurvePoint point) {
        buf.writeInt(n);

        for (CurvePoint.Part part : CurvePoint.Part.values()) {
            Vector3 position = point.get(part);
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
