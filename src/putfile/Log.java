package putfile;

public class Log {

    public static void e(String tag, String msg) {
        System.out.println();
        return;
    }
    public static void i(String tag, String msg) {
        System.out.println(String.format("INFO: %tc: %s: %s", System.currentTimeMillis(), tag, msg));
        return;
    }
    public static void w(String tag, String msg) {
        System.out.println(String.format("WARNING: %tc: %s: %s", System.currentTimeMillis(), tag, msg));
        return;
    }
}