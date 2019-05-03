package org.riversun.promise;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    private static boolean sLogEnabled = false;

    public static boolean isLogEnabled() {
        return sLogEnabled;
    }

    public static void setLogEnabled(boolean enabled) {

        sLogEnabled = enabled;

        if (enabled) {
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %5$s %6$s%n");
            Logger rootLogger = Logger.getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                handler.setLevel(Level.FINE);
            }
            rootLogger.setLevel(Level.FINE);
        }
    }

}
