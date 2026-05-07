package deliciousbread481.posestackdebugger;  
  
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
  
    public PoseStackDebugger() {  
        LOGGER.info("[PoseStackDebugger] Loaded - monitoring pushPose/popPose balance.");  
        initLogFile();  
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