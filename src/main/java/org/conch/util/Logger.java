/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.util;

import com.google.common.collect.Maps;
import org.apache.log4j.PropertyConfigurator;
import org.conch.Conch;
import org.conch.env.RuntimeEnvironment;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

/**
 * Handle logging for the Conch node server
 */
public final class Logger {

    /** Log event types */
    public enum Event {
        MESSAGE, EXCEPTION
    }

    /** Log levels */
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /** Message listeners */
    private static final Listeners<String, Event> messageListeners = new org.conch.util.Listeners<>();

    /** Exception listeners */
    private static final Listeners<Throwable, Event> exceptionListeners = new Listeners<>();

    /** Our logger instance */
    private static final org.slf4j.Logger log;

    /** Enable stack traces */
    private static final boolean enableStackTraces;

    /** Enable log traceback */
    private static final boolean enableLogTraceback;

    /**
     * No constructor
     */
    private Logger() {}
    
    static void appendPrefix(Properties loggingProperties){
        // append USER_HOME as prefix under desktop mode
        if(RuntimeEnvironment.isDesktopApplicationEnabled()) {
            String logFileName = "log4j.appender.D.File";
            String errorFileName = "log4j.appender.E.File";
            String logFilePath = Conch.getUserHomeDir() + File.separator + loggingProperties.getProperty(logFileName);
            String errorFilePath = Conch.getUserHomeDir() + File.separator + loggingProperties.getProperty(errorFileName);

            loggingProperties.setProperty(logFileName,logFilePath);
            loggingProperties.setProperty(errorFileName,errorFilePath);
        }
    }
    
    /**
     * Logger initialization
     *
     * The existing Java logging configuration will be used if the Java logger has already
     * been initialized.  Otherwise, we will configure our own log manager and log handlers.
     * The conf/logging-default.properties and conf/logging.properties configuration
     * files will be used.  Entries in logging.properties will override entries in
     * logging-default.properties.
     */
    static {
        String oldManager = System.getProperty("java.util.logging.manager");
        System.setProperty("java.util.logging.manager", "org.conch.util.ConchLogManager");
        if (!(LogManager.getLogManager() instanceof ConchLogManager)) {
            System.setProperty("java.util.logging.manager",
                    (oldManager != null ? oldManager : "java.util.logging.LogManager"));
        }
        
        Properties loggingProperties = new Properties();
        String defaultPropertieFile = Conch.getConfDir() + File.separator + "logging-default.properties";
        try (InputStream fis = new FileInputStream(defaultPropertieFile)) {
            loggingProperties.load(fis);
            appendPrefix(loggingProperties);
        } catch (IOException e) {
            System.err.println(String.format("Error loading default logging properties from %s", defaultPropertieFile));
        }
        PropertyConfigurator.configure(loggingProperties);
        
        if (! Boolean.getBoolean("sharder.doNotConfigureLogging")) {
            try {
                Conch.loadProperties(loggingProperties, "logging-default.properties", true);
                Conch.loadProperties(loggingProperties, "logging.properties", false);
                Conch.updateLogFileHandler(loggingProperties);
                if (loggingProperties.size() > 0) {
                    appendPrefix(loggingProperties);
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    loggingProperties.store(outStream, "logging properties");
                    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
                    java.util.logging.LogManager.getLogManager().readConfiguration(inStream);
                    inStream.close();
                    outStream.close();
                }
                BriefLogFormatter.init();
            } catch (IOException e) {
                throw new RuntimeException("Error loading logging properties", e);
            }
        }
        
        log = org.slf4j.LoggerFactory.getLogger(Conch.class);
        enableStackTraces = Conch.getBooleanProperty("sharder.enableStackTraces");
        enableLogTraceback = Conch.getBooleanProperty("sharder.enableLogTraceback");
        logInfoMessage("logging enabled");
    }

    /**
     * Logger initialization
     */
    public static void init() {}

    /**
     * Logger shutdown
     */
    public static void shutdown() {
        if (LogManager.getLogManager() instanceof ConchLogManager) {
            ((ConchLogManager) LogManager.getLogManager()).conchShutdown();
        }
    }

    /**
     * Set the log level
     *
     * @param       level               Desired log level
     */
    public static void setLevel(Level level) {
        java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger(log.getName());
        switch (level) {
            case DEBUG:
                jdkLogger.setLevel(java.util.logging.Level.FINE);
                break;
            case INFO:
                jdkLogger.setLevel(java.util.logging.Level.INFO);
                break;
            case WARN:
                jdkLogger.setLevel(java.util.logging.Level.WARNING);
                break;
            case ERROR:
                jdkLogger.setLevel(java.util.logging.Level.SEVERE);
                break;
        }
    }

    /**
     * Add a message listener
     *
     * @param       listener            Listener
     * @param       eventType           Notification event type
     * @return                          TRUE if listener added
     */
    public static boolean addMessageListener(Listener<String> listener, Event eventType) {
        return messageListeners.addListener(listener, eventType);
    }

    /**
     * Add an exception listener
     *
     * @param       listener            Listener
     * @param       eventType           Notification event type
     * @return                          TRUE if listener added
     */
    public static boolean addExceptionListener(Listener<Throwable> listener, Event eventType) {
        return exceptionListeners.addListener(listener, eventType);
    }

    /**
     * Remove a message listener
     *
     * @param       listener            Listener
     * @param       eventType           Notification event type
     * @return                          TRUE if listener removed
     */
    public static boolean removeMessageListener(Listener<String> listener, Event eventType) {
        return messageListeners.removeListener(listener, eventType);
    }

    /**
     * Remove an exception listener
     *
     * @param       listener            Listener
     * @param       eventType           Notification event type
     * @return                          TRUE if listener removed
     */
    public static boolean removeExceptionListener(Listener<Throwable> listener, Event eventType) {
        return exceptionListeners.removeListener(listener, eventType);
    }

    /**
     * Log a message (map to INFO)
     *
     * @param       message             Message
     */
    public static void logMessage(String message) {
        doLog(Level.INFO, message, null);
    }

    /**
     * Log an exception (map to ERROR)
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logMessage(String message, Exception exc) {
        doLog(Level.ERROR, message, exc);
    }

    public static void logShutdownMessage(String message) {
        if (LogManager.getLogManager() instanceof ConchLogManager) {
            logMessage(message);
        } else {
            System.out.println(message);
        }
    }

    public static void logShutdownMessage(String message, Exception e) {
        if (LogManager.getLogManager() instanceof ConchLogManager) {
            logMessage(message, e);
        } else {
            System.out.println(message);
            System.out.println(e.toString());
        }
    }

    public static boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    /**
     * Log an ERROR message
     *
     * @param       message             Message
     */
    public static void logErrorMessage(String message) {
        doLog(Level.ERROR, message, null);
    }

    /**
     * Log an ERROR exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logErrorMessage(String message, Throwable exc) {
        doLog(Level.ERROR, message, exc);
    }

    public static boolean isWarningEnabled() {
        return log.isWarnEnabled();
    }

    /**
     * Log a WARNING message
     *
     * @param       message             Message
     */
    public static void logWarningMessage(String message) {
        doLog(Level.WARN, message, null);
    }

    public static void logWarningMessage(String format, Object ... args) {
        doLog(Level.WARN, String.format(format, args), null);
    }

    /**
     * Log a WARNING exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logWarningMessage(String message, Throwable exc) {
        doLog(Level.WARN, message, exc);
    }

    public static boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    /**
     * Log an INFO message
     *
     * @param       message             Message
     */
    public static void logInfoMessage(String message) {
        doLog(Level.INFO, message, null);
    }

    /**
     * Log an INFO message
     *
     * @param       format             Message format
     * @param       args               Message args
     */
    public static void logInfoMessage(String format, Object ... args) {
        doLog(Level.INFO, String.format(format, args), null);
    }

    /**
     * Log an INFO exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logInfoMessage(String message, Throwable exc) {
        doLog(Level.INFO, message, exc);
    }

    public static boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    /**
     * Log a debug message
     *
     * @param       message             Message
     */
    public static void logDebugMessage(String message) {
        doLog(Level.DEBUG, message, null);
    }

    /**
     * Log a debug message
     *
     * @param       format             Message format
     * @param       args               Message args
     */
    public static void logDebugMessage(String format, Object ... args) {
        doLog(Level.DEBUG, String.format(format, args), null);
    }

    /**
     * Log a debug exception
     *
     * @param       message             Message
     * @param       exc                 Exception
     */
    public static void logDebugMessage(String message, Throwable exc) {
        doLog(Level.DEBUG, message, exc);
    }

    /**
     * Log the event
     *
     * @param       level               Level
     * @param       message             Message
     * @param       exc                 Exception
     */
    private static void doLog(Level level, String message, Throwable exc) {
        String logMessage = message;
        Throwable e = exc;
        //
        // Add caller class and method if enabled
        //
        if (enableLogTraceback) {
            StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
            String className = caller.getClassName();
            int index = className.lastIndexOf('.');
            if (index != -1) {
                className = className.substring(index+1);
            }
            logMessage = className + "." + caller.getMethodName() + "#" + caller.getLineNumber() + ": " + logMessage;
        }
        //
        // Format the stack trace if enabled
        //
        if (e != null) {
            if (!enableStackTraces) {
                logMessage = logMessage + "\n" + exc.toString();
                e = null;
            }
        }
        //
        // Log the event
        //
        switch (level) {
            case DEBUG:
                log.debug(logMessage, e);
                break;
            case INFO:
                log.info(logMessage, e);
                break;
            case WARN:
                log.warn(logMessage, e);
                break;
            case ERROR:
                log.error(logMessage, e);
                break;
            default:
                break;
        }
        //
        // Notify listeners
        //
        if (exc != null)
            exceptionListeners.notify(exc, Event.EXCEPTION);
        else
            messageListeners.notify(message, Event.MESSAGE);
    }
    
    private  static final int DEFAULT_PRINT_COUNT = 100;
    static Map<String, Integer> logControlMap = Maps.newHashMap();
    static Map<String, Integer> printCountMap = Maps.newHashMap();


    public synchronized static void modifyControlCount(Class clazz, int printCount){
        logControlMap.put(clazz.getName(), printCount);
    }

    /**
     * init the control count setting, just execute once
     * @param clazz
     * @param printCount
     */
    public synchronized static void initControlCount(Class clazz, int printCount){
        String key =  clazz.getName();
        if(logControlMap.containsKey(key)) return;
        
        logControlMap.put(key, printCount);
        printCountMap.put(key, new Integer(0));
    }

    /**
     * init the control count setting and check whether printNow 
     * @param clazz
     * @param controlCount
     * @return
     */
    public static boolean printNow(Class clazz, int controlCount){
        initControlCount(clazz,controlCount);
        return printNow(clazz);
    }

    /**
     * check whether printNow 
     * 
     * @param clazz
     * @return
     */
    public static boolean printNow(Class clazz){
        String key =  clazz.getName();
        if(!logControlMap.containsKey(key)) {
            logControlMap.put(key, DEFAULT_PRINT_COUNT);
            printCountMap.put(key, new Integer(0));
        }
        int controlCount = logControlMap.get(key).intValue();
        int curCount = printCountMap.get(key).intValue();
        boolean printNow = curCount++ == 0 || curCount++ > controlCount;
        if(curCount > 1 && printNow) {
            printCountMap.put(key, 0);
        }else {
            printCountMap.put(key, curCount);
        }
       
        return printNow;
    } 
}
