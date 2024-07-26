package me.kous500.curvebuilding.fabric.client;

import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.fabric.client.render.RenderPreview;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class CurveBuildingFabricClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PosDataPayload.ID, (payload, context) -> {
            ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
            if (clientPlayer == null) return;

            context.client().execute(() -> RenderPreview.posData = new PosData(payload.sendPosData()));
        });

        ServerPlayConnectionEvents.JOIN.register(((networkHandler, sender, server) -> RenderPreview.posData = null));
    }
}
