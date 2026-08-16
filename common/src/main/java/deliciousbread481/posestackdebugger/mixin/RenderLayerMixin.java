package deliciousbread481.posestackdebugger.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class RenderLayerMixin {
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"
            )
    )
    private void posestackdebugger$wrapLayerRender(RenderLayer<?, ?> layer, PoseStack pose, MultiBufferSource buffer, int light, Entity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, Operation<Void> original) {
        int before = PoseStackDebugger.depthOf(pose);
        original.call(layer, pose, buffer, light, entity,
                limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        int after = PoseStackDebugger.depthOf(pose);
        if (before != after) {
            StringBuilder sb = new StringBuilder();
            sb.append("渲染层 ").append(layer.getClass().getName())
              .append(" 导致深度变化: ").append(before).append(" -> ").append(after)
              .append("，实体=").append(entity.getClass().getName());
            if (entity instanceof LivingEntity living) {
                sb.append("\n  主手: ").append(posestackdebugger$describeItem(living.getMainHandItem()));
                sb.append("\n  副手: ").append(posestackdebugger$describeItem(living.getOffhandItem()));
            }
            sb.append("\n");
            PoseStackDebugger.log("LAYER IMBALANCE", sb.toString());
        }
    }

    private static String posestackdebugger$describeItem(ItemStack stack) {
        if (stack.isEmpty()) return "<空>";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}