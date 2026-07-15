package deliciousbread481.posestackdebugger.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderLayerMixin {
    @WrapOperation(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;"
                            + "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                            + "ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
            )
    )
    private void posestackdebugger$wrapLayerSubmit(
            RenderLayer<?, ?> layer,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int light,
            EntityRenderState renderState,
            float yRot,
            float xRot,
            Operation<Void> original
    ) {
        int before = PoseStackDebugger.depthOf(poseStack);
        original.call(
                layer,
                poseStack,
                submitNodeCollector,
                light,
                renderState,
                yRot,
                xRot
        );
        int after = PoseStackDebugger.depthOf(poseStack);
        if (before != after) {
            PoseStackDebugger.log("LAYER IMBALANCE",
                    "渲染层 " + layer.getClass().getName()
                            + " 导致深度变化: " + before + " -> " + after
                            + "，实体=" + renderState.entityType + "\n");
        }
    }
}
