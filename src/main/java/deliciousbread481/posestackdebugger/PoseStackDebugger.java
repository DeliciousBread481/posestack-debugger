package deliciousbread481.posestackdebugger;  
  
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent; 
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.common.Mod;  
import net.minecraftforge.fml.loading.FMLPaths;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
  
import java.io.*;  
import java.nio.file.Path;  
import java.time.LocalDateTime;  
import java.time.format.DateTimeFormatter;  
  
@Mod("posestackdebugger")  
public class PoseStackDebugger {  
  
    public static final Logger LOGGER = LoggerFactory.getLogger("PoseStackDebugger");  
    private static final Object LOCK = new Object();  
    private static PrintWriter writer;  
    
    public static volatile int currentGuiDepth = 0;
    public static volatile Object guiPoseStackInstance = null;
    private int snapshotDepth = 0;  
  
    public PoseStackDebugger() {  
        LOGGER.info("[PoseStackDebugger] Loaded - monitoring pushPose/popPose balance.");  
        initLogFile();     
        MinecraftForge.EVENT_BUS.register(this);
    }  
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        guiPoseStackInstance = e.getGuiGraphics().pose();
        snapshotDepth = currentGuiDepth;
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenRenderPost(ScreenEvent.Render.Post e) {
        int now = currentGuiDepth;
        if (now != snapshotDepth) {
            log("SCREEN IMBALANCE",
                    "Screen " + e.getScreen().getClass().getName()
                            + " 的 renderWithTooltip 导致深度变化: "
                            + snapshotDepth + " -> " + now + "\n"
                            + "（元凶是该 Screen 本体或包裹它的 mixin）\n");
        }
    }
  
    private static void initLogFile() {  
        try {  
            Path logDir = FMLPaths.GAMEDIR.get().resolve("logs");  
            File logFile = logDir.resolve("posestack-debugger.log").toFile();  
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)), true);  
            writer.println("=== PoseStackDebugger session started at " +  
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===");  
            writer.flush();  
        } catch (IOException e) {  
            LOGGER.error("[PoseStackDebugger] Failed to create log file!", e);  
        }  
    }  
  
    public static void log(String message) {  
        synchronized (LOCK) {  
            if (writer != null) {  
                writer.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "] " + message);  
                writer.flush();  
            }  
        }  
    }  
  
    public static void log(String header, String body) {  
        synchronized (LOCK) {  
            if (writer != null) {  
                writer.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "] " + header);  
                writer.println(body);  
                writer.flush();  
            }  
        }  
    }  
}