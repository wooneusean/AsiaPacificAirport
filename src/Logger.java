import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    static void log(String who, String message) {
        System.out.printf("[%s][%s] %s > %s%n", getTime(), Thread.currentThread().getName(), who, message);
    }

    static String getTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return formatter.format(LocalTime.now());
    }
}
