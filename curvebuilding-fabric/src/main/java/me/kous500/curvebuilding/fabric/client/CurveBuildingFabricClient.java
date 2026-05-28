package me.kous500.curvebuilding.fabric.client;

import me.kous500.curvebuilding.PosData;
import me.kous500.curvebuilding.fabric.network.BezierPosPayload;
import me.kous500.curvebuilding.fabric.network.PosDataPacket;
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
        ClientPlayNetworking.registerGlobalReceiver(BezierPosPayload.ID, (client, handler, posBuf, responseSender) -> {
            ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;

            if (clientPlayer == null) return;
            RenderPreview.posData = new PosData(PosDataPacket.readPosPacket(posBuf));
        });

        ServerPlayConnectionEvents.JOIN.register(((networkHandler, sender, server) -> RenderPreview.posData = null));
    }
}
