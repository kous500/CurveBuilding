package me.kous500.curvebuilding.fabric.client;

import me.kous500.curvebuilding.fabric.network.PosDataPayload;
import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.fabric.client.render.CurveBuildingRenderTypes;
import me.kous500.curvebuilding.fabric.client.render.RenderPreview;
import me.kous500.curvebuilding.math.PosManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=26.1 {
/*import me.kous500.curvebuilding.fabric.client.render.Render;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.player.LocalPlayer;

public class CurveBuildingFabricClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PosDataPayload.ID, (payload, context) -> {
            LocalPlayer clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer == null) return;

            context.client().execute(() -> RenderPreview.posData = new PosData(payload.sendPosData()));
        });

        ClientLoginConnectionEvents.INIT.register((ClientHandshakePacketListenerImpl handler, Minecraft client) -> {
            PosManager.clearPosMap();
            RenderPreview.posData = null;
        });

        CurveBuildingRenderTypes.init();
        //? if >=26.1 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            Render.beginFrame(context.submitNodeCollector(), context.poseStack());
            RenderPreview.RenderPosData(context.poseStack().last().pose());
        });
        *///?} else {
        WorldRenderEvents.END_MAIN.register(context -> RenderPreview.RenderPosData(context.matrices().last().pose()));
        //?}
    }
}
