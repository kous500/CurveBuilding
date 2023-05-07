package me.kous500.curvebuilding.fabric.client.render.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.kous500.curvebuilding.fabric.client.render.RenderPreview.RenderPosData;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
    void renderer_postWorldRender(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        RenderPosData(matrix);
    }
}