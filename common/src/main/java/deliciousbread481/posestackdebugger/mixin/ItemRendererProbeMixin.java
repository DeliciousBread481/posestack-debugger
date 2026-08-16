package deliciousbread481.posestackdebugger.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererProbeMixin {

    @Unique
    private static final ThreadLocal<ArrayDeque<int[]>> posestackdebugger$depths =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
        method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At("HEAD")
    )
    private void posestackdebugger$pre(ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                       PoseStack pose, MultiBufferSource buf, int light, int overlay,
                                       BakedModel model, CallbackInfo ci) {
        posestackdebugger$depths.get().push(new int[]{ PoseStackDebugger.depthOf(pose) });
    }

    @Inject(
        method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At("RETURN")
    )
    private void posestackdebugger$post(ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                        PoseStack pose, MultiBufferSource buf, int light, int overlay,
                                        BakedModel model, CallbackInfo ci) {
        ArrayDeque<int[]> s = posestackdebugger$depths.get();
        if (s.isEmpty()) return;
        int before = s.pop()[0];
        int after = PoseStackDebugger.depthOf(pose);
        if (before != after) {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            PoseStackDebugger.log("ITEM IMBALANCE",
                    "物品 " + id + " 渲染导致深度变化: " + before + " -> " + after
                            + "，displayContext=" + ctx + "\n");
        }
    }
}