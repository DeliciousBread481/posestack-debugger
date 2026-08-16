package deliciousbread481.posestackdebugger.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class BewlrProbeMixin {

    @Unique
    private static final ThreadLocal<ArrayDeque<int[]>> posestackdebugger$bewlrDepths =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void posestackdebugger$pre(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                                       MultiBufferSource buf, int light, int overlay, CallbackInfo ci) {
        posestackdebugger$bewlrDepths.get().push(new int[]{ PoseStackDebugger.depthOf(pose) });
    }

    @Inject(method = "renderByItem", at = @At("RETURN"))
    private void posestackdebugger$post(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                                        MultiBufferSource buf, int light, int overlay, CallbackInfo ci) {
        ArrayDeque<int[]> s = posestackdebugger$bewlrDepths.get();
        if (s.isEmpty()) return;
        int before = s.pop()[0];
        int after = PoseStackDebugger.depthOf(pose);
        if (before != after) {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            PoseStackDebugger.log("BEWLR IMBALANCE",
                    "渲染器 " + this.getClass().getName()
                            + " 渲染物品 " + id + " 导致深度变化: " + before + " -> " + after
                            + "，displayContext=" + ctx + "\n");
        }
    }
}