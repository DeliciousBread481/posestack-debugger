package deliciousbread481.posestackdebugger.mixin;  
  
import deliciousbread481.posestackdebugger.PoseStackDebugger;  
import com.mojang.blaze3d.vertex.PoseStack;  
import org.spongepowered.asm.mixin.Mixin;  
import org.spongepowered.asm.mixin.Unique;  
import org.spongepowered.asm.mixin.injection.At;  
import org.spongepowered.asm.mixin.injection.Inject;  
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;  

import java.util.ArrayDeque;
  
@Mixin(PoseStack.class)  
public abstract class PoseStackMixin {  
  
    @Unique  
    private int posestackdebugger$depth = 0;  
    
    @Unique
    private final ArrayDeque<String> posestackdebugger$owners = new ArrayDeque<>();
    
    @Unique
    private boolean posestackdebugger$shouldTrack() {
        if (!Thread.currentThread().getName().equals("Render thread")) {
            return false;
        }
        Object gui = PoseStackDebugger.guiPoseStackInstance;
        return gui == null || (Object) this == gui;
    }
    
    @Unique
    private String posestackdebugger$firstModCaller() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : trace) {
            String c = e.getClassName();
            if (!c.startsWith("com.mojang.")
                    && !c.startsWith("net.minecraft.")
                    && !c.startsWith("net.minecraftforge.")
                    && !c.startsWith("org.spongepowered.")
                    && !c.startsWith("deliciousbread481.posestackdebugger.")
                    && !c.startsWith("java.")
                    && !c.startsWith("sun.")
                    && !c.startsWith("jdk.")) {
                return c + "#" + e.getMethodName() + ":" + e.getLineNumber();
            }
        }
        return "<vanilla/forge>";
    }
  
    @Inject(method = "pushPose", at = @At("HEAD"))  
    private void posestackdebugger$onPushPose(CallbackInfo ci) {  
        if (!posestackdebugger$shouldTrack()) return;
        posestackdebugger$depth++;  
        posestackdebugger$owners.push(posestackdebugger$firstModCaller());
        PoseStackDebugger.currentGuiDepth = posestackdebugger$depth;
    }  
  
    @Inject(method = "popPose", at = @At("HEAD"))  
    private void posestackdebugger$onPopPose(CallbackInfo ci) {  
        if (!posestackdebugger$shouldTrack()) return;
        posestackdebugger$depth--;  
        PoseStackDebugger.currentGuiDepth = posestackdebugger$depth;
        
        String matchedOwner = posestackdebugger$owners.isEmpty()
                ? "<none>" : posestackdebugger$owners.pop();
        String popper = posestackdebugger$firstModCaller();
  
        if (posestackdebugger$depth < 0 || !popper.equals(matchedOwner)) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== POSESTACK IMBALANCE DETECTED ===\n");
            sb.append("depth: ").append(posestackdebugger$depth).append("\n");
            sb.append("pop 调用方:        ").append(popper).append("\n");
            sb.append("被弹层的 push 调用方: ").append(matchedOwner).append("\n");
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
            PoseStackDebugger.log("IMBALANCE", sb.toString());
            if (posestackdebugger$depth < 0) {
                posestackdebugger$depth = 0;
                posestackdebugger$owners.clear();
                PoseStackDebugger.currentGuiDepth = 0;
            }
        }
    }  
  
    @Inject(method = "pushPose", at = @At("TAIL"))  
    private void posestackdebugger$onPushPoseTail(CallbackInfo ci) {  
        if (!posestackdebugger$shouldTrack()) return;
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