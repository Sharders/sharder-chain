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
import org.conch.chain.BlockchainProcessorImpl;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.env.RuntimeEnvironment;
import org.conch.http.API;
import org.conch.mint.Generator;
import org.conch.peer.Peers;

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
    
    private static String level;
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
        String defaultPropertieFile = null;
        try{
            defaultPropertieFile = Conch.getConfDir() + File.separator + "logging-default.properties";
            InputStream fis = new FileInputStream(defaultPropertieFile);
            loggingProperties.load(fis);
            appendPrefix(loggingProperties);
            PropertyConfigurator.configure(loggingProperties);
        }catch (IOException e) {
            System.err.println(String.format("Error loading default logging properties from %s caused by %s", defaultPropertieFile, e.getMessage()));
        }catch (Exception e) {
            System.err.println(String.format("Error loading default logging properties from %s caused by %s", defaultPropertieFile, e.getMessage()));
        }catch (Throwable throwable) {
            System.err.println(String.format("Error loading default logging properties from %s caused by %s", defaultPropertieFile, throwable.getMessage()));
        }
        
        if(defaultPropertieFile == null) {
            // console logger 
            loggingProperties.put("log4j.rootLogger", "debug,stdout");
            loggingProperties.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
            loggingProperties.put("log4j.appender.stdout.Target", "System.out");
            loggingProperties.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
            loggingProperties.put("log4j.appender.stdout.layout.ConversionPattern", "%-d{yyyy-MM-dd HH:mm:ss} [ %p ][ %t:%r ] - %l:  %m%n");
            PropertyConfigurator.configure(loggingProperties);
            BriefLogFormatter.init();
        }else if(!Boolean.getBoolean("sharder.doNotConfigureLogging")) {
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
                level = loggingProperties.getProperty("log4j.appender.D.Threshold");
                BriefLogFormatter.init();
            } catch (IOException e) {
                throw new RuntimeException("Error loading logging properties", e);
            }
        }

        Class loggerClass = defaultPropertieFile != null ? Conch.class : Logger.class;
        log = org.slf4j.LoggerFactory.getLogger(loggerClass);
        
        enableStackTraces = (defaultPropertieFile != null) && Conch.getBooleanProperty("sharder.enableStackTraces");
        enableLogTraceback = (defaultPropertieFile != null) && Conch.getBooleanProperty("sharder.enableLogTraceback");
        
  
        logInfoMessage("logging enabled");
    }

    /**
     * Logger initialization
     */
    public static void init() { }

    /**
     * Logger shutdown
     */
    public static void shutdown() {
        if (LogManager.getLogManager() instanceof ConchLogManager) {
            ((ConchLogManager) LogManager.getLogManager()).conchShutdown();
        }
    }
    
    public static boolean isLevel(Level checkLevel){
        switch (checkLevel) {
            case DEBUG:
                return "DEBUG".equalsIgnoreCase(level);
            case INFO:
                return "INFO".equalsIgnoreCase(level);
            case WARN:
                return "WARN".equalsIgnoreCase(level);
            case ERROR:
                return "ERROR".equalsIgnoreCase(level);
        }
        return false;
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

    public static void logErrorMessage(String format, Object ... args) {
        doLog(Level.ERROR, String.format(format, args), null);
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
     * @param message Message
     * @param exc Exception
     */
    public static void logDebugMessage(String message, Throwable exc) {
        doLog(Level.DEBUG, message, exc);
    }

    /**
     * Call stack
     *
     * @return
     */
    public static String callStack() {
        String stacks = String.format("[DEBUG] Call stacks detail(thread name=%s, thread id=%d): \n",
                Thread.currentThread().getName(), Thread.currentThread().getId());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            if (i <= 1) {
                continue;
            }
            stacks += String.format("[DEBUG] %s#%s($%s:%d)\n", elements[i].getClassName(),
                    elements[i].getMethodName(), elements[i].getFileName(), elements[i].getLineNumber());
        }
        return stacks;
    }


    /**
     * Log the event
     *
     * @param level Level
     * @param message Message
     * @param exc Exception
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
    
    private  static final int DEFAULT_PRINT_COUNT = 50;
    static Map<String, Integer> logControlMap = Maps.newHashMap();
    static Map<String, Integer> printCountMap = Maps.newHashMap();


    public synchronized static void modifyControlCount(Class clazz, int printCount){
        logControlMap.put(clazz.getName(), printCount);
    }

    /**
     * init the control count setting, just execute once
     * @param key
     * @param printCount
     */
    public synchronized static void initControlCount(String key, int printCount){
        if(logControlMap.containsKey(key)) return;
        
        logControlMap.put(key, printCount);
        printCountMap.put(key, new Integer(0));
    }
    
    
    // FIXME a) add annotation for method to defined the print count check
    //       auto check according to method map
    //       b) add a new method isPrintNow
    /**
     * init the control count setting and check whether printNow 
     * @param key
     * @param controlCount
     * @return
     */
    public static boolean printNow(String key, int controlCount){
        initControlCount(key,controlCount);
        return printNow(key);
    }

    /**
     * check whether printNow 
     * 
     * @param key
     * @return
     */
    public static boolean printNow(String key){
//        String key =  clazz.getName();
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

    /** log count check key **/
    public static final String Generator_getNextGenerators = Generator.class.getName() + "#getNextGenerators";
    public static final String Generator_isMintHeightReached = Generator.class.getName() + "#isMintHeightReached";
    public static final String Generator_checkOrStartAutoMining = Generator.class.getName() + "#checkOrStartAutoMining";
    public static final String Generator_isBlockStuck = Generator.class.getName() + "#isBlockStuck";
    public static final String Generator_isPocTxsProcessed = Generator.class.getName() + "#isPocTxsProcessed";
    public static final String Generator_startMining = Generator.class.getName() + "#startMining";
    public static final String CONCH_P_reachLastKnownBlock = Conch.class.getName() + "#reachLastKnownBlock";
    public static final String CONCH_P_readAndSetSerialNum = Conch.class.getName() + "#readAndSetSerialNum";
    public static final String BlockchainProcessor_downloadPeer_sizeCheck = BlockchainProcessorImpl.class.getName() + "#downloadPeer#sizeCheck";
    public static final String BlockchainProcessor_downloadPeer_getWeightedPeer = BlockchainProcessorImpl.class.getName() + "#downloadPeer#getWeightedPeer";
    public static final String BlockchainProcessor_getMoreBlocks = BlockchainProcessorImpl.class.getName() + "#getMoreBlocks";
    public static final String BlockchainProcessor_oldPocTxsProcessingCheck = BlockchainProcessorImpl.class.getName() + "#oldPocTxsProcessingCheck";
    public static final String GetNodeHardware_P_report = GetNodeHardware.class.getName() + "#report";
    public static final String IpUtil_geoTransformFailed = IpUtil.class.getName() + "#geoTransformFailed";
    public static final String API_incorrectAdmPwd = API.class.getName() + "#incorrectAdmPwd";
    public static final String PEERS_CHECK_OR_CONNECT_TO_PEER = Peers.class.getName() + "#_connectToPeer";
}
