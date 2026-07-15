package deliciousbread481.posestackdebugger;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Deque;

public final class PoseStackDebugger {

    public static final String MOD_ID = "posestackdebugger";
    public static final Logger LOGGER = LoggerFactory.getLogger("PoseStackDebugger");

    private static final Object LOCK = new Object();
    private static PrintWriter writer;

    private static final long MAX_LOG_BYTES = 20L * 1024 * 1024;
    private static long writtenBytes = 0;
    private static boolean capReached = false;
    private static String lastSignature = null;
    private static int duplicateCount = 0;

    public static volatile int currentGuiDepth = 0;
    public static volatile Object guiPoseStackInstance = null;
    public static volatile String currentEntity = "<none>";

    private static int snapshotDepth = 0;
    private static int livingDepthSnapshot = 0;

    private static Field POSE_DEQUE_FIELD;
    private static boolean initialized = false;

    private PoseStackDebugger() {
    }
    
    public static void init() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("[PoseStackDebugger] Loaded - monitoring pushPose/popPose balance.");
        initLogFile();
    }

    public static int depthOf(PoseStack poseStack) {
        try {
            if (POSE_DEQUE_FIELD == null) {
                for (Field f : PoseStack.class.getDeclaredFields()) {
                    if (Deque.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        POSE_DEQUE_FIELD = f;
                        break;
                    }
                }
            }
            if (POSE_DEQUE_FIELD == null) return -1;
            Object deque = POSE_DEQUE_FIELD.get(poseStack);
            return (deque instanceof Deque) ? ((Deque<?>) deque).size() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    public static void onScreenRenderPre(PoseStack guiPose) {
        guiPoseStackInstance = guiPose;
        snapshotDepth = currentGuiDepth;
    }

    public static void onScreenRenderPost(Object screen) {
        int now = currentGuiDepth;
        if (now != snapshotDepth) {
            log("SCREEN IMBALANCE",
                    "Screen " + screen.getClass().getName()
                            + " 的 render 导致深度变化: "
                            + snapshotDepth + " -> " + now + "\n"
                            + "（元凶是该 Screen 本体或包裹它的 mixin）\n");
        }
    }

    public static void onRenderLivingPre(PoseStack pose, Object entity) {
        livingDepthSnapshot = depthOf(pose);
        currentEntity = entity.getClass().getName();
    }

    public static void onRenderLivingPost(PoseStack pose, Object entity) {
        int now = depthOf(pose);
        if (now != livingDepthSnapshot) {
            log("LIVING EVENT IMBALANCE",
                    "实体 " + entity.getClass().getName()
                            + " 渲染期间深度变化: " + livingDepthSnapshot + " -> " + now
                            + "\n（元凶是该实体的某个渲染层）\n");
        }
    }

    private static void initLogFile() {
        try {
            Path logDir = Platform.getGameFolder().resolve("logs");
            File logFile = logDir.resolve("posestack-debugger.log").toFile();
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)), true);
            writtenBytes = 0;
            capReached = false;
            writer.println("=== PoseStackDebugger session started at " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===");
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("[PoseStackDebugger] Failed to create log file!", e);
        }
    }

    private static void writeRaw(String text) {
        if (writer == null || capReached) return;
        writer.print(text);
        writer.flush();
        writtenBytes += text.getBytes(StandardCharsets.UTF_8).length;
        if (writtenBytes >= MAX_LOG_BYTES) {
            capReached = true;
            writer.println("=== LOG SIZE CAP (" + (MAX_LOG_BYTES / 1024 / 1024)
                    + " MB) REACHED - further logging suppressed ===");
            writer.flush();
        }
    }

    private static void logDeduped(String signature, String fullText) {
        synchronized (LOCK) {
            if (writer == null || capReached) return;
            if (signature.equals(lastSignature)) {
                duplicateCount++;
                return;
            }
            if (duplicateCount > 0) {
                writeRaw("[" + ts() + "] ... 上一条重复出现 " + duplicateCount + " 次，已折叠\n");
                duplicateCount = 0;
            }
            lastSignature = signature;
            writeRaw(fullText);
        }
    }

    private static String ts() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    public static void log(String message) {
        logDeduped(message, "[" + ts() + "] " + message + "\n");
    }

    public static void log(String header, String body) {
        logDeduped(header + "\n" + body, "[" + ts() + "] " + header + "\n" + body + "\n");
    }
}