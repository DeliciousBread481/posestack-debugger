package deliciousbread481.posestackdebugger.mixin;  
  
import deliciousbread481.posestackdebugger.PoseStackDebugger;  
import com.mojang.blaze3d.vertex.PoseStack;  
import org.spongepowered.asm.mixin.Mixin;  
import org.spongepowered.asm.mixin.Unique;  
import org.spongepowered.asm.mixin.injection.At;  
import org.spongepowered.asm.mixin.injection.Inject;  
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;  
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
  
@Mixin(PoseStack.class)  
public abstract class PoseStackMixin {  
  
    @Unique  
    private int posestackdebugger$depth = 0;  
    
    @Unique
    private final ArrayDeque<String> posestackdebugger$owners = new ArrayDeque<>();
    
    @Unique
    private boolean posestackdebugger$shouldTrack() {
        return Thread.currentThread().getName().equals("Render thread");
    }
    
    @Unique
    private boolean posestackdebugger$isGuiInstance() {
        Object gui = PoseStackDebugger.guiPoseStackInstance;
        return gui != null && (Object) this == gui;
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
    
    @Unique
    private String posestackdebugger$collectTrace() {
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }
  
    @Inject(method = "pushPose", at = @At("HEAD"))  
    private void posestackdebugger$onPushPose(CallbackInfo ci) {  
        if (!posestackdebugger$shouldTrack()) return;
        posestackdebugger$depth++;  
        posestackdebugger$owners.push(posestackdebugger$firstModCaller());
        if (posestackdebugger$isGuiInstance()) {
            PoseStackDebugger.currentGuiDepth = posestackdebugger$depth;
        }
    }  
  
    @Inject(method = "popPose", at = @At("HEAD"))  
    private void posestackdebugger$onPopPose(CallbackInfo ci) {  
        if (!posestackdebugger$shouldTrack()) return;
        posestackdebugger$depth--;  
        if (posestackdebugger$isGuiInstance()) {
            PoseStackDebugger.currentGuiDepth = posestackdebugger$depth;
        }
        
        String matchedOwner = posestackdebugger$owners.isEmpty()
                ? "<none>" : posestackdebugger$owners.pop();
        String popper = posestackdebugger$firstModCaller();
  
        if (posestackdebugger$depth < 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== POSESTACK UNDERFLOW (popPose) DETECTED ===\n");
            sb.append("instance: ").append(posestackdebugger$isGuiInstance()
                    ? "GUI" : "NON-GUI(世界/阴影渲染等)").append("\n");
            sb.append("depth: ").append(posestackdebugger$depth).append("\n");
            sb.append("pop 调用方:        ").append(popper).append("\n");
            sb.append("被弹层的 push 调用方: ").append(matchedOwner).append("\n");
            sb.append("Stack trace:\n");
            sb.append(posestackdebugger$collectTrace());
            
            PoseStackDebugger.log("UNDERFLOW", sb.toString());
            
            posestackdebugger$depth = 0;
            posestackdebugger$owners.clear();
            if (posestackdebugger$isGuiInstance()) {
                PoseStackDebugger.currentGuiDepth = 0;
            }
        }
    }
    
    @Inject(method = "last", at = @At("HEAD"))
    private void posestackdebugger$onLast(CallbackInfoReturnable<PoseStack.Pose> cir) {
        if (!posestackdebugger$shouldTrack()) return;
        if (posestackdebugger$depth < 0) {
            String reader = posestackdebugger$firstModCaller();
            StringBuilder sb = new StringBuilder();
            sb.append("=== POSESTACK EMPTY READ (last) DETECTED ===\n");
            sb.append("instance: ").append(posestackdebugger$isGuiInstance()
                    ? "GUI" : "NON-GUI(世界/阴影渲染等)").append("\n");
            sb.append("depth: ").append(posestackdebugger$depth)
                    .append("  (真实栈已空，last()/getLast() 即将抛 NoSuchElementException)\n");
            sb.append("空栈读取调用方: ").append(reader).append("\n");
            sb.append("Stack trace:\n");
            sb.append(posestackdebugger$collectTrace());
            
            PoseStackDebugger.log("EMPTY READ", sb.toString());
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
            sb.append(posestackdebugger$collectTrace());
            
            PoseStackDebugger.log("OVERFLOW WARNING", sb.toString());
        }
    }
}