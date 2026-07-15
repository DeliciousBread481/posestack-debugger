package deliciousbread481.posestackdebugger.neoforge;

import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PoseStackDebugger.MOD_ID, dist = Dist.CLIENT)
public final class PoseStackDebuggerNeoForge {
    public PoseStackDebuggerNeoForge() {
        PoseStackDebugger.initialize(FMLPaths.GAMEDIR.get());
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        PoseStackDebugger.onScreenRenderPre(
                event.getScreen(),
                event.getGuiGraphics().pose()
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        PoseStackDebugger.onScreenRenderPost(
                event.getScreen(),
                event.getGuiGraphics().pose()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderLivingPre(RenderLivingEvent.Pre<?, ?, ?> event) {
        PoseStackDebugger.onLivingRenderPre(event.getRenderState(), event.getPoseStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderLivingPost(RenderLivingEvent.Post<?, ?, ?> event) {
        PoseStackDebugger.onLivingRenderPost(event.getRenderState(), event.getPoseStack());
    }
}
