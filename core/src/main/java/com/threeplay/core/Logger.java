package com.threeplay.core;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.StringBuilderPrinter;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliranbe on 5/16/16.
 */
public class Logger {
    private static Logger defaultLogger = new Logger();
    public final static int NONE = 0;
    public final static int INFO = 1<<0;
    public final static int WARN = 1<<1;
    public final static int ERROR = 1<<2;
    public final static int DEBUG = 1<<3;
    public final static int ALL = INFO|WARN|ERROR|DEBUG;

    private List<Reporter> reporters = new LinkedList<>();

    protected static void setDefaultLogger(Logger logger){
        defaultLogger = logger;
    }

    public static Logger defaultLogger(){
        return defaultLogger;
    }

    public interface Reporter {
        void log(int level, String message);
    }

    public void log(int level, String logString, Object... args){
        String message = String.format(logString, args);
        for (Reporter reporter: reporters) {
            reporter.log(level, message);
        }
    }

    public void attachReporter(Reporter reporter){
        reporters.add(reporter);
    }

    public static void l(int level, String logString, Object... args){
        defaultLogger.log(level, logString, args);
    }

    public static void i(String logString, Object... args){
        defaultLogger.info(logString, args);
    }

    public static void w(String logString, Object... args){
        defaultLogger.warning(logString, args);
    }
    public static void e(String logString, Object... args){
        defaultLogger.error(logString, args);
    }

    public static void d(String logString, Object... args){
        defaultLogger.debug(logString, args);
    }

    public void info(String logString, Object... args){
        log(INFO, logString, args);
    }

    public void warning(String logString, Object... args){
        log(WARN, logString, args);
    }

    public void error(String logString, Object... args){
        log(ERROR, logString, args);
    }

    public void debug(String logString, Object... args){
        log(DEBUG, logString, args);
    }

    public Writer writer(final int level, final String logString) {
        return new Writer() {
            private StringBuilder messageBuffer = new StringBuilder();
            private boolean flushed = false;
            @Override
            public void write(@NonNull char[] chars, int off, int len) throws IOException {
                messageBuffer.append(chars, off, len);
                flushed = false;
            }

            @Override
            public void flush() throws IOException {
                if ( !flushed ) {
                    if ( logString != null ) {
                        log(level, "%s\n%s", logString, messageBuffer.toString());
                    }
                    else {
                        log(level, "%s", messageBuffer.toString());
                    }
                    messageBuffer = new StringBuilder();
                }
            }

            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }

    public static class ConsoleReporter implements Reporter {
        private final String tag;

        public ConsoleReporter(){
            this("logger");
        }

        public ConsoleReporter(String tag){
            this.tag = tag;
        }

        @Override
        public void log(int level, String message) {
            if ( (level & ERROR) != 0 ) Log.e(tag, message);
            else if ( (level & WARN) != 0 ) Log.w(tag, message);
            else if ( (level & DEBUG) != 0 ) Log.d(tag, message);
            else if ( (level & INFO) != 0 ) Log.i(tag, message);
        }
    }
}
