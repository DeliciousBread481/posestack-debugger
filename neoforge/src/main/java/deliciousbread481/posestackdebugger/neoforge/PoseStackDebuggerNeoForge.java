package deliciousbread481.posestackdebugger.neoforge;

import deliciousbread481.posestackdebugger.PoseStackDebugger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = PoseStackDebugger.MOD_ID, dist = Dist.CLIENT)
public final class PoseStackDebuggerNeoForge {
    public PoseStackDebuggerNeoForge() {
        PoseStackDebugger.init();
    }
}