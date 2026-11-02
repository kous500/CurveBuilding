package me.kous500.curvebuilding.fabric.client;

import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.fabric.client.render.PreviewBackends;
import me.kous500.curvebuilding.fabric.client.render.RenderPreview;
import me.kous500.curvebuilding.math.PosManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.player.LocalPlayer;

/**
 * The mod on the client side, which draws the preview.
 */
public class CurveBuildingFabricClient implements ClientModInitializer {
    /**
     * Receives the positions that the server sends for the preview, forgets them when the client connects to a server,
     * and starts the preview.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PosDataPayload.ID, (payload, context) -> {
            LocalPlayer clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer == null) return;

            context.client().execute(() -> RenderPreview.posData = new PosData(payload.sendPosData()));
        });

        ClientLoginConnectionEvents.INIT.register((ClientHandshakePacketListenerImpl handler, Minecraft client) -> {
            // In single player the server runs in this game and keeps its positions from one world to the next
            PosManager.clearPosMap();
            RenderPreview.posData = null;
        });

        PreviewBackends.init(RenderPreview::render);
    }
}
