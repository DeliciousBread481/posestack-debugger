package deliciousbread481.posestackdebugger.mixin;

import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class ForgeHooksClientMixin {
    
    @Inject(
            method = "drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void posestackdebugger$onDrawScreenHead(Screen screen, GuiGraphics guiGraphics,int mouseX, int mouseY, float partialTick,CallbackInfo ci) {
        PoseStackDebugger.guiPoseStackInstance = guiGraphics.pose();
    }
}