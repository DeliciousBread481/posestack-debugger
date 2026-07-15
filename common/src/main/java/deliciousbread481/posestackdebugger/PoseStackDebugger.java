package deliciousbread481.posestackdebugger;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Matrix3x2fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PoseStackDebugger {
    public static final String MOD_ID = "posestackdebugger";
    public static final Logger LOGGER = LoggerFactory.getLogger("PoseStackDebugger");

    private static final Object LOCK = new Object();
    private static final long MAX_LOG_BYTES = 20L * 1024 * 1024;
    private static final DateTimeFormatter SESSION_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LOG_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final ThreadLocal<Map<Screen, Integer>> SCREEN_DEPTHS =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Deque<LivingSnapshot>> LIVING_DEPTHS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private static PrintWriter writer;
    private static long writtenBytes;
    private static boolean capReached;
    private static String lastSignature;
    private static int duplicateCount;
    private static Field poseIndexField;
    private static Field guiIndexField;

    public static volatile int currentGuiDepth;
    public static volatile Object guiTransformStackInstance;
    public static volatile String currentEntity = "<none>";

    private PoseStackDebugger() {
    }

    public static void initialize(Path gameDirectory) {
        LOGGER.info("[PoseStackDebugger] Loaded - monitoring rendering stack balance.");
        initLogFile(gameDirectory);
    }

    public static int depthOf(PoseStack poseStack) {
        if (poseStack instanceof StackDepthTracker tracker) {
            return tracker.posestackdebugger$getDepth();
        }

        try {
            if (poseIndexField == null) {
                for (Field field : PoseStack.class.getDeclaredFields()) {
                    if (field.getType() == int.class) {
                        field.setAccessible(true);
                        poseIndexField = field;
                        break;
                    }
                }
            }
            return poseIndexField == null ? -1 : poseIndexField.getInt(poseStack);
        } catch (Throwable throwable) {
            return -1;
        }
    }

    public static int guiDepthOf(Matrix3x2fStack stack) {
        try {
            if (guiIndexField == null) {
                for (Field field : Matrix3x2fStack.class.getDeclaredFields()) {
                    if (field.getType() == int.class) {
                        field.setAccessible(true);
                        guiIndexField = field;
                        break;
                    }
                }
            }
            return guiIndexField == null ? -1 : guiIndexField.getInt(stack);
        } catch (Throwable throwable) {
            return -1;
        }
    }

    public static void captureGuiStack(Matrix3x2fStack stack) {
        guiTransformStackInstance = stack;
        int depth = guiDepthOf(stack);
        if (depth >= 0) {
            currentGuiDepth = depth;
        }
    }

    public static void onScreenRenderPre(Screen screen, Matrix3x2fStack stack) {
        captureGuiStack(stack);
        SCREEN_DEPTHS.get().put(screen, guiDepthOf(stack));
    }

    public static void onScreenRenderPost(Screen screen, Matrix3x2fStack stack) {
        Integer before = SCREEN_DEPTHS.get().remove(screen);
        int after = guiDepthOf(stack);
        currentGuiDepth = after;
        if (before != null && after != before) {
            log("SCREEN IMBALANCE",
                    "Screen " + screen.getClass().getName()
                            + " 的 extractRenderStateWithTooltipAndSubtitles 导致 GUI 矩阵深度变化: "
                            + before + " -> " + after + "\n"
                            + "（元凶是该 Screen 本体、事件监听器或包裹它的 mixin）\n");
        }
    }

    public static void onLivingRenderPre(EntityRenderState renderState, PoseStack poseStack) {
        int depth = depthOf(poseStack);
        String entity = entityName(renderState);
        LIVING_DEPTHS.get().push(new LivingSnapshot(poseStack, depth, entity));
        currentEntity = entity;
    }

    public static void onLivingRenderPost(EntityRenderState renderState, PoseStack poseStack) {
        LivingSnapshot snapshot = popLivingSnapshot(poseStack);
        if (snapshot == null) {
            return;
        }

        int after = depthOf(poseStack);
        if (after != snapshot.depth()) {
            log("LIVING EVENT IMBALANCE",
                    "实体 " + entityName(renderState)
                            + " 渲染期间深度变化: " + snapshot.depth() + " -> " + after
                            + "\n（元凶是该实体的某个渲染层或实体渲染回调）\n");
        }

        Deque<LivingSnapshot> remaining = LIVING_DEPTHS.get();
        currentEntity = remaining.isEmpty() ? "<none>" : remaining.peek().entity();
    }

    private static LivingSnapshot popLivingSnapshot(PoseStack poseStack) {
        Deque<LivingSnapshot> snapshots = LIVING_DEPTHS.get();
        if (snapshots.isEmpty()) {
            return null;
        }
        if (snapshots.peek().poseStack() == poseStack) {
            return snapshots.pop();
        }

        for (LivingSnapshot snapshot : List.copyOf(snapshots)) {
            if (snapshot.poseStack() == poseStack) {
                snapshots.remove(snapshot);
                return snapshot;
            }
        }
        return null;
    }

    private static String entityName(EntityRenderState renderState) {
        return renderState.entityType == null ? "<unknown>" : renderState.entityType.toString();
    }

    private static void initLogFile(Path gameDirectory) {
        synchronized (LOCK) {
            try {
                Path logDirectory = gameDirectory.resolve("logs");
                Files.createDirectories(logDirectory);
                Path logFile = logDirectory.resolve("posestack-debugger.log");
                if (writer != null) {
                    writer.close();
                }
                BufferedWriter bufferedWriter = Files.newBufferedWriter(
                        logFile,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
                writer = new PrintWriter(bufferedWriter, true);
                writtenBytes = 0;
                capReached = false;
                lastSignature = null;
                duplicateCount = 0;
                writeRaw("=== PoseStackDebugger session started at "
                        + LocalDateTime.now().format(SESSION_TIME) + " ===\n");
            } catch (IOException exception) {
                LOGGER.error("[PoseStackDebugger] Failed to create log file!", exception);
            }
        }
    }

    private static void writeRaw(String text) {
        if (writer == null || capReached) {
            return;
        }
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
            if (writer == null || capReached) {
                return;
            }
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
        return LocalDateTime.now().format(LOG_TIME);
    }

    public static void log(String message) {
        logDeduped(message, "[" + ts() + "] " + message + "\n");
    }

    public static void log(String header, String body) {
        logDeduped(header + "\n" + body, "[" + ts() + "] " + header + "\n" + body + "\n");
    }

    private record LivingSnapshot(PoseStack poseStack, int depth, String entity) {
    }
}
