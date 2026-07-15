package deliciousbread481.posestackdebugger.fabric;

import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class PoseStackDebuggerFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PoseStackDebugger.initialize(FabricLoader.getInstance().getGameDir());
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.beforeExtract(screen).register(
                    (currentScreen, graphics, mouseX, mouseY, partialTick) ->
                            PoseStackDebugger.onScreenRenderPre(
                                    currentScreen,
                                    graphics.pose()
                            )
            );
            ScreenEvents.afterExtract(screen).register(
                    (currentScreen, graphics, mouseX, mouseY, partialTick) ->
                            PoseStackDebugger.onScreenRenderPost(
                                    currentScreen,
                                    graphics.pose()
                            )
            );
        });
    }
}
