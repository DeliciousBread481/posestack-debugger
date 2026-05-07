package deliciousbread481.posestackdebugger.mixin;  
  
import deliciousbread481.posestackdebugger.PoseStackDebugger;  
import com.mojang.blaze3d.vertex.PoseStack;  
import org.spongepowered.asm.mixin.Mixin;  
import org.spongepowered.asm.mixin.Unique;  
import org.spongepowered.asm.mixin.injection.At;  
import org.spongepowered.asm.mixin.injection.Inject;  
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;  
  
@Mixin(PoseStack.class)  
public abstract class PoseStackMixin {  
  
    @Unique  
    private int posestackdebugger$depth = 0;  
  
    @Inject(method = "pushPose", at = @At("HEAD"))  
    private void posestackdebugger$onPushPose(CallbackInfo ci) {  
        posestackdebugger$depth++;  
    }  
  
    @Inject(method = "popPose", at = @At("HEAD"))  
    private void posestackdebugger$onPopPose(CallbackInfo ci) {  
        posestackdebugger$depth--;  
  
        if (posestackdebugger$depth < 0) {  
            StringBuilder sb = new StringBuilder();  
            sb.append("===POSESTACK UNDERFLOW DETECTED ===\n");  
            sb.append("popPose() called without matching pushPose()! Depth: ").append(posestackdebugger$depth).append("\n");  
            sb.append("Stack trace:\n");  
  
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();  
            for (int i = 2; i < stackTrace.length && i < 40; i++) {  
                StackTraceElement element = stackTrace[i];  
                String className = element.getClassName();  
                sb.append("  at ").append(element).append("\n");  
  
                if (!className.startsWith("com.mojang.")  
                        && !className.startsWith("net.minecraft.")  
                        && !className.startsWith("net.minecraftforge.")  
                        && !className.startsWith("org.spongepowered.")  
                        && !className.startsWith("deliciousbread481.posestackdebugger.")  
                        && !className.startsWith("java.")  
                        && !className.startsWith("sun.")  
                        && !className.startsWith("jdk.")) {  
                    sb.append("  >>> SUSPECT MOD CLASS: ").append(className).append(" <<<\n");  
                }  
            }  
            sb.append("============\n");  
  
            PoseStackDebugger.log("UNDERFLOW", sb.toString());  
  
            posestackdebugger$depth = 0;  
        }  
    }  
  
    @Inject(method = "pushPose", at = @At("TAIL"))  
    private void posestackdebugger$onPushPoseTail(CallbackInfo ci) {  
        if (posestackdebugger$depth > 64) {  
            StringBuilder sb = new StringBuilder();  
            sb.append("=== POSESTACK OVERFLOW WARNING ===\n");  
            sb.append("pushPose() depth abnormally high: ").append(posestackdebugger$depth).append("\n");  
            sb.append("Stack trace:\n");  
  
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();  
            for (int i = 2; i < stackTrace.length && i < 30; i++) {  
                StackTraceElement element = stackTrace[i];  
                String className = element.getClassName();  
                sb.append("  at ").append(element).append("\n");  
  
                if (!className.startsWith("com.mojang.")  
                        && !className.startsWith("net.minecraft.")  
                        && !className.startsWith("net.minecraftforge.")  
                        && !className.startsWith("org.spongepowered.")  
                        && !className.startsWith("deliciousbread481.posestackdebugger.")  
                        && !className.startsWith("java.")  
                        && !className.startsWith("sun.")  
                        && !className.startsWith("jdk.")) {  
                    sb.append("  >>> SUSPECT MOD CLASS: ").append(className).append(" <<<\n");  
                }  
            }  
            sb.append("============\n");  
  
            PoseStackDebugger.log("OVERFLOW WARNING", sb.toString());  
        }  
    }  
}