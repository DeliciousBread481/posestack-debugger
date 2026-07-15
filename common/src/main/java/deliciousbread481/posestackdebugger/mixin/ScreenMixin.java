package deliciousbread481.posestackdebugger.mixin;

import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void posestackdebugger$onRenderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        PoseStackDebugger.onScreenRenderPre(guiGraphics.pose());
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void posestackdebugger$onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        PoseStackDebugger.onScreenRenderPost(this);
    }
}