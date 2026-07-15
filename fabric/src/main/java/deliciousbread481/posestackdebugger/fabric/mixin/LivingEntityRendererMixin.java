package deliciousbread481.posestackdebugger.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/"
                    + "LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void posestackdebugger$beforeLivingSubmit(
            LivingEntityRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo callbackInfo
    ) {
        PoseStackDebugger.onLivingRenderPre(renderState, poseStack);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/"
                    + "LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void posestackdebugger$afterLivingSubmit(
            LivingEntityRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo callbackInfo
    ) {
        PoseStackDebugger.onLivingRenderPost(renderState, poseStack);
    }
}
