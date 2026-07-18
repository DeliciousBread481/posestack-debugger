package deliciousbread481.posestackdebugger.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import deliciousbread481.posestackdebugger.PoseStackDebugger;
import deliciousbread481.posestackdebugger.StackDepthTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@Mixin(PoseStack.class)
public abstract class PoseStackMixin implements StackDepthTracker {
    @Unique
    private int posestackdebugger$depth;

    @Unique
    private final ArrayDeque<String> posestackdebugger$owners = new ArrayDeque<>();

    @Override
    public int posestackdebugger$getDepth() {
        return posestackdebugger$depth;
    }

    @Override
    public List<String> posestackdebugger$snapshotOwners(int count) {
        List<String> result = new ArrayList<>();
        if (count <= 0) {
            return result;
        }
        for (String owner : posestackdebugger$owners) {
            if (result.size() >= count) {
                break;
            }
            result.add(owner);
        }
        return result;
    }

    @Unique
    private boolean posestackdebugger$shouldTrack() {
        return Thread.currentThread().getName().equals("Render thread");
    }

    @Unique
    private boolean posestackdebugger$isGuiInstance() {
        Object gui = PoseStackDebugger.guiTransformStackInstance;
        return gui != null && (Object) this == gui;
    }

    @Unique
    private String posestackdebugger$firstModCaller() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : trace) {
            String className = element.getClassName();
            if (!posestackdebugger$isInfrastructureClass(className)) {
                return className + "#" + element.getMethodName() + ":" + element.getLineNumber();
            }
        }
        return "<vanilla/loader>";
    }

    @Unique
    private String posestackdebugger$collectTrace() {
        StringBuilder builder = new StringBuilder();
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (int index = 2; index < trace.length && index < 30; index++) {
            StackTraceElement element = trace[index];
            String className = element.getClassName();
            builder.append("  at ").append(element).append("\n");
            if (!posestackdebugger$isInfrastructureClass(className)) {
                builder.append("  >>> SUSPECT MOD CLASS: ").append(className).append(" <<<\n");
            }
        }
        return builder.toString();
    }

    @Unique
    private boolean posestackdebugger$isInfrastructureClass(String className) {
        return className.startsWith("com.mojang.")
                || className.startsWith("com.llamalad7.")
                || className.startsWith("net.minecraft.")
                || className.startsWith("net.minecraftforge.")
                || className.startsWith("net.neoforged.")
                || className.startsWith("net.fabricmc.")
                || className.startsWith("org.joml.")
                || className.startsWith("org.spongepowered.")
                || className.startsWith("deliciousbread481.posestackdebugger.")
                || className.startsWith("java.")
                || className.startsWith("sun.")
                || className.startsWith("jdk.");
    }

    @Inject(method = "pushPose", at = @At("HEAD"))
    private void posestackdebugger$onPushPose(CallbackInfo callbackInfo) {
        if (!posestackdebugger$shouldTrack()) {
            return;
        }
        posestackdebugger$depth++;
        posestackdebugger$owners.push(posestackdebugger$firstModCaller());
        posestackdebugger$updateGuiDepth();
    }

    @Inject(method = "popPose", at = @At("HEAD"))
    private void posestackdebugger$onPopPose(CallbackInfo callbackInfo) {
        if (!posestackdebugger$shouldTrack()) {
            return;
        }
        posestackdebugger$depth--;
        posestackdebugger$updateGuiDepth();

        String matchedOwner = posestackdebugger$owners.isEmpty()
                ? "<none>" : posestackdebugger$owners.pop();
        String popper = posestackdebugger$firstModCaller();

        if (posestackdebugger$depth < 0) {
            StringBuilder builder = new StringBuilder();
            builder.append("=== POSESTACK UNDERFLOW (popPose) DETECTED ===\n");
            builder.append("instance: ").append(posestackdebugger$isGuiInstance()
                    ? "GUI" : "NON-GUI(世界/阴影渲染等)").append("\n");
            builder.append("depth: ").append(posestackdebugger$depth).append("\n");
            builder.append("pop 调用方:        ").append(popper).append("\n");
            builder.append("被弹层的 push 调用方: ").append(matchedOwner).append("\n");
            builder.append("Stack trace:\n");
            builder.append(posestackdebugger$collectTrace());
            PoseStackDebugger.log("UNDERFLOW", builder.toString());

            posestackdebugger$depth = 0;
            posestackdebugger$owners.clear();
            posestackdebugger$updateGuiDepth();
        }
    }

    @Inject(method = "last", at = @At("HEAD"))
    private void posestackdebugger$onLast(
            CallbackInfoReturnable<PoseStack.Pose> callbackInfoReturnable
    ) {
        if (!posestackdebugger$shouldTrack() || posestackdebugger$depth >= 0) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("=== POSESTACK EMPTY READ (last) DETECTED ===\n");
        builder.append("instance: ").append(posestackdebugger$isGuiInstance()
                ? "GUI" : "NON-GUI(世界/阴影渲染等)").append("\n");
        builder.append("depth: ").append(posestackdebugger$depth)
                .append("  (真实栈已空，last() 即将失败)\n");
        builder.append("空栈读取调用方: ").append(posestackdebugger$firstModCaller()).append("\n");
        builder.append("Stack trace:\n");
        builder.append(posestackdebugger$collectTrace());
        PoseStackDebugger.log("EMPTY READ", builder.toString());
    }

    @Inject(method = "pushPose", at = @At("TAIL"))
    private void posestackdebugger$onPushPoseTail(CallbackInfo callbackInfo) {
        if (!posestackdebugger$shouldTrack() || posestackdebugger$depth <= 64) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("=== POSESTACK OVERFLOW WARNING ===\n");
        builder.append("pushPose() depth abnormally high: ")
                .append(posestackdebugger$depth).append("\n");
        builder.append("Stack trace:\n");
        builder.append(posestackdebugger$collectTrace());
        PoseStackDebugger.log("OVERFLOW WARNING", builder.toString());
    }

    @Unique
    private void posestackdebugger$updateGuiDepth() {
        if (posestackdebugger$isGuiInstance()) {
            PoseStackDebugger.currentGuiDepth = posestackdebugger$depth;
        }
    }
}
