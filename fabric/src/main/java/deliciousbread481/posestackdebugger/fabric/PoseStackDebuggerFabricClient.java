package deliciousbread481.posestackdebugger.fabric;

import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.fabricmc.api.ClientModInitializer;

public final class PoseStackDebuggerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PoseStackDebugger.init();
    }
}