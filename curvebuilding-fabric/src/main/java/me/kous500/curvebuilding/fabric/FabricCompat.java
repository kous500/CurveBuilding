package me.kous500.curvebuilding.fabric;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.world.World;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
//? if >=1.21.11 {
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * Code that differs between Minecraft and WorldEdit versions, outside of rendering.
 * Rendering differences are in the preview backends instead (see PreviewBackends).
 * <ul>
 *   <li>1.21.11: ResourceLocation became Identifier, and permission levels became permission checks.</li>
 *   <li>26.1: WorldEdit 7.4 adapters are instance methods, and PayloadTypeRegistry.playS2C became clientboundPlay.</li>
 * </ul>
 */
public final class FabricCompat {
    private FabricCompat() {
    }

    public static Player adaptPlayer(ServerPlayer player) {
        //? if >=26.1 {
        return FabricAdapter.get().fromNativePlayer(player);
        //?} else {
        /*return FabricAdapter.adaptPlayer(player);
        *///?}
    }

    public static World adaptWorld(Level level) {
        //? if >=26.1 {
        return FabricAdapter.get().fromNativeWorld(level);
        //?} else {
        /*return FabricAdapter.adapt(level);
        *///?}
    }

    /**
     * Whether the command source has the permission level of a game master (operator level 2).
     */
    public static boolean isGameMaster(CommandSourceStack source) {
        //? if >=1.21.11 {
        return Commands.LEVEL_GAMEMASTERS.check(source.permissions());
        //?} else {
        /*return source.hasPermission(2);
        *///?}
    }

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        //? if >=1.21.11 {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("curvebuilding", path));
        //?} else {
        /*return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("curvebuilding", path));
        *///?}
    }

    public static <T extends CustomPacketPayload> void registerClientboundPayload(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec) {
        //? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        //?} else {
        /*PayloadTypeRegistry.playS2C().register(type, codec);
        *///?}
    }
}
