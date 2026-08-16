package deliciousbread481.posestackdebugger.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerProbeMixin {

    @Unique
    private static final ThreadLocal<ArrayDeque<int[]>> posestackdebugger$counters =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD")
    )
    private void posestackdebugger$pre(PoseStack pose, MultiBufferSource buffer, int light, LivingEntity entity,
                                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                       float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!"Render thread".equals(Thread.currentThread().getName())) return;
        posestackdebugger$counters.get().push(new int[]{ PoseStackDebugger.depthOf(pose), 0, 0 });
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("RETURN")
    )
    private void posestackdebugger$post(PoseStack pose, MultiBufferSource buffer, int light, LivingEntity entity,
                                        float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                        float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!"Render thread".equals(Thread.currentThread().getName())) return;
        ArrayDeque<int[]> stack = posestackdebugger$counters.get();
        if (stack.isEmpty()) return;
        int[] c = stack.pop();
        int before = c[0];
        int pushCount = c[1];
        int popCount = c[2];
        int after = PoseStackDebugger.depthOf(pose);
        if (before != after) {
            PoseStackDebugger.log("ITEMINHAND IMBALANCE",
                    "ItemInHandLayer.render 内部深度变化: " + before + " -> " + after
                            + "，本层 push=" + pushCount + " pop=" + popCount
                            + "，实体=" + entity.getClass().getName()
                            + (pushCount > popCount
                            ? "\n  （push 多于 pop：有 " + (pushCount - popCount) + " 次 popPose 被 mod 跳过）"
                            : "") + "\n");
        }
    }

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V")
    )
    private void posestackdebugger$wrapPush(PoseStack instance, Operation<Void> original) {
        ArrayDeque<int[]> stack = posestackdebugger$counters.get();
        if (!stack.isEmpty()) stack.peek()[1]++;
        original.call(instance);
    }

    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V")
    )
    private void posestackdebugger$wrapPop(PoseStack instance, Operation<Void> original) {
        ArrayDeque<int[]> stack = posestackdebugger$counters.get();
        if (!stack.isEmpty()) stack.peek()[2]++;
        StringBuilder sb = new StringBuilder("ItemInHandLayer.popPose() 被调用，调用链：\n");
        for (StackTraceElement e : new Throwable().getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            boolean suspect = (m.contains("redirect$") || m.contains("wrapOperation")
                    || m.contains("handler$") || m.contains("$mixinextras$"))
                    && !c.startsWith("deliciousbread481.posestackdebugger.");
            sb.append(suspect ? "  >>> " : "      ").append(e)
              .append(suspect ? "   <<< SUSPECT" : "").append('\n');
        }
        PoseStackDebugger.log("ITEMINHAND POP", sb.toString());
        original.call(instance);
    }
}