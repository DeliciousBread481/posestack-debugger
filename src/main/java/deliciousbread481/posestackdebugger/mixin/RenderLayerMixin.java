package deliciousbread481.posestackdebugger.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {

    @Unique
    private int posestackdebugger$depthBefore;
    
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",
            at = @At("HEAD")
    )
    private void posestackdebugger$onLayerRenderHead(PoseStack poseStack, Object buffer, int light,
                                                     Object entity, float p5, float p6, float p7,
                                                     float p8, float p9, float p10, CallbackInfo ci) {
        posestackdebugger$depthBefore = PoseStackDebugger.depthOf(poseStack);
    }
    
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",
            at = @At("RETURN")
    )
    private void posestackdebugger$onLayerRenderReturn(PoseStack poseStack, Object buffer, int light,
                                                       Object entity, float p5, float p6, float p7,
                                                       float p8, float p9, float p10, CallbackInfo ci) {
        int after = PoseStackDebugger.depthOf(poseStack);
        if (after != posestackdebugger$depthBefore) {
            PoseStackDebugger.log("LAYER IMBALANCE",
                    "渲染层 " + this.getClass().getName()
                            + " 导致深度变化: " + posestackdebugger$depthBefore + " -> " + after
                            + "\n当前渲染实体: " + PoseStackDebugger.currentEntity + "\n");
        }
    }
}