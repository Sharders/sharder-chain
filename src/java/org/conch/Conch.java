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

package org.conch;

import org.apache.commons.io.FileUtils;
import org.conch.addons.AddOns;
import org.conch.crypto.Crypto;
import org.conch.db.DbBackup;
import org.conch.env.DirProvider;
import org.conch.env.RuntimeEnvironment;
import org.conch.env.RuntimeMode;
import org.conch.env.ServerStatus;
import org.conch.http.API;
import org.conch.http.APIProxy;
import org.conch.peer.Peers;
import org.conch.storage.StorageManager;
import org.conch.user.Users;
import org.conch.util.*;
import org.json.simple.JSONObject;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.security.AccessControlException;
import java.time.LocalDateTime;
import java.util.*;

public final class Conch {

    public static final String VERSION = "0.1.0";
    public static final String APPLICATION = "COS";

    private static volatile Time time = new Time.EpochTime();

    public static final String CONCH_DEFAULT_PROPERTIES = "sharder-default.properties";
    public static final String CONCH_PROPERTIES = "sharder.properties";
    public static final String CONFIG_DIR = "conf";

    private static final RuntimeMode runtimeMode;
    private static final DirProvider dirProvider;

    private static final Properties defaultProperties = new Properties();
    static {
        redirectSystemStreams("out");
        redirectSystemStreams("err");
        System.out.println("Initializing COS server version " + Conch.VERSION);
        printCommandLineArguments();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        System.out.printf("Runtime mode %s\n", runtimeMode.getClass().getName());
        dirProvider = RuntimeEnvironment.getDirProvider();
        System.out.println("User home folder " + dirProvider.getUserHomeDir());
        loadProperties(defaultProperties, CONCH_DEFAULT_PROPERTIES, true);

        boolean checkVersion = Boolean.valueOf(Conch.defaultProperties.getProperty("sharder.version"));
        if (checkVersion && !VERSION.equals(Conch.defaultProperties.getProperty("sharder.version"))) {
            throw new RuntimeException("Using an sharder-default.properties file from a version other than " + VERSION + " is not supported!!!");
        }
    }

    private static void redirectSystemStreams(String streamName) {
        String isStandardRedirect = System.getProperty("sharder.redirect.system." + streamName);
        Path path = null;
        if (isStandardRedirect != null) {
            try {
                path = Files.createTempFile("sharder.system." + streamName + ".", ".log");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            String explicitFileName = System.getProperty("sharder.system." + streamName);
            if (explicitFileName != null) {
                path = Paths.get(explicitFileName);
            }
        }
        if (path != null) {
            try {
                PrintStream stream = new PrintStream(Files.newOutputStream(path));
                if (streamName.equals("out")) {
                    System.setOut(new PrintStream(stream));
                } else {
                    System.setErr(new PrintStream(stream));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final Properties properties = new Properties(defaultProperties);

    static {
        loadProperties(properties, CONCH_PROPERTIES, false);
    }

    public static void storePropertiesToFile(HashMap<String, String> parameters) {

        OutputStream output = null;
        Properties userProperties = loadProperties(properties, CONCH_PROPERTIES, false);;
        parameters.entrySet().forEach(map -> userProperties.setProperty(map.getKey(), map.getValue()));
        try {
            output = new FileOutputStream("conf/" + CONCH_PROPERTIES);
            LocalDateTime now = LocalDateTime.now();
            userProperties.store(output , "Updated by HubConfig Manager " + now.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                output.flush();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault) {
        try {
            // Load properties from location specified as command line parameter
            String configFile = System.getProperty(propertiesFile);
            if (configFile != null) {
                System.out.printf("Loading %s from %s\n", propertiesFile, configFile);
                try (InputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Error loading %s from %s", propertiesFile, configFile));
                }
            } else {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
                    // When running sharder.exe from a Windows installation we always have sharder.properties in the classpath but this is not the sharder properties file
                    // Therefore we first load it from the classpath and then look for the real sharder.properties in the user folder.
                    if (is != null) {
                        System.out.printf("Loading %s from classpath\n", propertiesFile);
                        properties.load(is);
                        if (isDefault) {
                            return properties;
                        }
                    }
                    // load non-default properties files from the user folder
                    if (!dirProvider.isLoadPropertyFileFromUserDir()) {
                        return properties;
                    }
                    String homeDir = dirProvider.getUserHomeDir();
                    if (!Files.isReadable(Paths.get(homeDir))) {
                        System.out.printf("Creating dir %s\n", homeDir);
                        try {
                            Files.createDirectory(Paths.get(homeDir));
                        } catch(Exception e) {
                            if (!(e instanceof NoSuchFileException)) {
                                throw e;
                            }
                            // Fix for WinXP and 2003 which does have a roaming sub folder
                            Files.createDirectory(Paths.get(homeDir).getParent());
                            Files.createDirectory(Paths.get(homeDir));
                        }
                    }
                    Path confDir = Paths.get(homeDir, CONFIG_DIR);
                    if (!Files.isReadable(confDir)) {
                        System.out.printf("Creating dir %s\n", confDir);
                        Files.createDirectory(confDir);
                    }
                    Path propPath = Paths.get(confDir.toString()).resolve(Paths.get(propertiesFile));
                    if (Files.isReadable(propPath)) {
                        System.out.printf("Loading %s from dir %s\n", propertiesFile, confDir);
                        properties.load(Files.newInputStream(propPath));
                    } else {
                        System.out.printf("Creating property file %s\n", propPath);
                        Files.createFile(propPath);
                        Files.write(propPath, Convert.toBytes("# use this file for workstation specific " + propertiesFile));
                    }
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error loading " + propertiesFile, e);
                }
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace(); // make sure we log this exception
            throw e;
        }
    }

    private static void printCommandLineArguments() {
        try {
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (inputArguments != null && inputArguments.size() > 0) {
                System.out.println("Command line arguments");
            } else {
                return;
            }
            inputArguments.forEach(System.out::println);
        } catch (AccessControlException e) {
            System.out.println("Cannot read input arguments " + e.getMessage());
        }
    }

    public static int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    public static int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined or not numeric, using default value " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined");
            return defaultValue;
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment)attachment);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws ConchException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    public static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws ConchException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws ConchException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }

    /**
     * @return 当前时间距离创世的秒数
     */
    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Conch.time = time;
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Conch::shutdown));
            init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        Logger.logShutdownMessage("Shutting down...");
        AddOns.shutdown();
        API.shutdown();
        Users.shutdown();
        FundingMonitor.shutdown();
        ThreadPool.shutdown();
        BlockchainProcessorImpl.getInstance().shutdown();
        Peers.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("COS server " + VERSION + " stopped.");
        Logger.shutdown();
        runtimeMode.shutdown();
    }

    private static class Init {

        private static volatile boolean initialized = false;

        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                setSystemProperties();
                logSystemProperties();
                runtimeMode.init();
                Thread secureRandomInitThread = initSecureRandom();
                setServerStatus(ServerStatus.BEFORE_DATABASE, null);
                Db.init();
                setServerStatus(ServerStatus.AFTER_DATABASE, null);
                StorageManager.init();
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                AccountRestrictions.init();
                AccountLedger.init();
                Alias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                PhasingPoll.init();
                Trade.init();
                AssetTransfer.init();
                AssetDelete.init();
                AssetDividend.init();
                Vote.init();
                PhasingVote.init();
                Currency.init();
                CurrencyBuyOffer.init();
                CurrencySellOffer.init();
                CurrencyFounder.init();
                CurrencyMint.init();
                CurrencyTransfer.init();
                Exchange.init();
                ExchangeRequest.init();
                Shuffling.init();
                ShufflingParticipant.init();
                PrunableMessage.init();
                TaggedData.init();
                StorageProcessorImpl.init();
                StorageBackup.init();
                FxtDistribution.init();
                Peers.init();
                APIProxy.init();
                Generator.init();
                AddOns.init();
                API.init();
//                HubConfig.init();
                Users.init();
                ForgePool.init();
                DebugTrace.init();
                // DbBackup Init
                DbBackup.init();
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Conch.getIntProperty("sharder.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Conch.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }
                try {
                    secureRandomInitThread.join(10000);
                } catch (InterruptedException ignore) {}
                testSecureRandom();
                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage("COS server " + VERSION + " started successfully.");
                Logger.logMessage("Copyright © 2017 sharder.org.");
                Logger.logMessage("Copyright © 2014-2017 ichaoj.com.");
                Logger.logMessage("Distributed under MIT.");
                if (API.getWelcomePageUri() != null) {
                    Logger.logMessage("Client UI is at " + API.getWelcomePageUri());
                }
                setServerStatus(ServerStatus.STARTED, API.getWelcomePageUri());
                if (isDesktopApplicationEnabled()) {
                    launchDesktopApplication();
                }
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
                // [Hub] if owner binded then start forge automatic
                Boolean hubBind = Conch.getBooleanProperty("sharder.HubBind");
                String hubBindAddress = Convert.emptyToNull(Conch.getStringProperty("sharder.HubBindAddress"));
                String hubBindPassPhrase = Convert.emptyToNull(Conch.getStringProperty("sharder.HubBindPassPhrase", "", true));
                if (hubBind && hubBindPassPhrase != null) {
                    Generator hubGenerator = Generator.startForging(hubBindPassPhrase.trim());
                    if(hubGenerator != null && (hubGenerator.getAccountId() != Convert.parseAccountId(hubBindAddress))) {
                        Generator.stopForging(hubBindPassPhrase.trim());
                        Logger.logInfoMessage("Account" + hubBindAddress + " is not same with Generator's passphrase");
                    } else {
                        Logger.logInfoMessage("Account " + hubBindAddress + "started forging automatically");
                    }
                }

            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                runtimeMode.alert(e.getMessage() + "\n" +
                        "See additional information in " + dirProvider.getLogFileDir() + System.getProperty("file.separator") + "sharder.log");
                System.exit(1);
            }
        }

        private static void init() {
            if (initialized) {
                throw new RuntimeException("Conch.init has already been called");
            }
            initialized = true;
        }

        private Init() {} // never

    }

    private static void setSystemProperties() {
      // Override system settings that the user has define in sharder.properties file.
      String[] systemProperties = new String[] {
        "socksProxyHost",
        "socksProxyPort",
      };

      for (String propertyName : systemProperties) {
        String propertyValue;
        if ((propertyValue = getStringProperty(propertyName)) != null) {
          System.setProperty(propertyName, propertyValue);
        }
      }
    }

    private static void logSystemProperties() {
        String[] loggedProperties = new String[] {
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding",
                "java.security.policy",
                "java.security.manager",
                RuntimeEnvironment.RUNTIME_MODE_ARG,
                RuntimeEnvironment.DIRPROVIDER_ARG
        };
        for (String property : loggedProperties) {
            Logger.logDebugMessage(String.format("%s = %s", property, System.getProperty(property)));
        }
        Logger.logDebugMessage(String.format("availableProcessors = %s", Runtime.getRuntime().availableProcessors()));
        Logger.logDebugMessage(String.format("maxMemory = %s", Runtime.getRuntime().maxMemory()));
        Logger.logDebugMessage(String.format("processId = %s", getProcessId()));
    }

    private static Thread initSecureRandom() {
        Thread secureRandomInitThread = new Thread(() -> Crypto.getSecureRandom().nextBytes(new byte[1024]));
        secureRandomInitThread.setDaemon(true);
        secureRandomInitThread.start();
        return secureRandomInitThread;
    }

    private static void testSecureRandom() {
        Thread thread = new Thread(() -> Crypto.getSecureRandom().nextBytes(new byte[1024]));
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(2000);
            if (thread.isAlive()) {
                throw new RuntimeException("SecureRandom implementation too slow!!! " +
                        "Install haveged if on linux, or set sharder.useStrongSecureRandom=false.");
            }
        } catch (InterruptedException ignore) {}
    }

    public static String getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (runtimeName == null) {
            return "";
        }
        String[] tokens = runtimeName.split("@");
        if (tokens.length == 2) {
            return tokens[0];
        }
        return "";
    }

    public static String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir);
    }

    public static void updateLogFileHandler(Properties loggingProperties) {
        dirProvider.updateLogFileHandler(loggingProperties);
    }

    public static String getUserHomeDir() {
        return dirProvider.getUserHomeDir();
    }

    public static File getConfDir() {
        return dirProvider.getConfDir();
    }

    private static void setServerStatus(ServerStatus status, URI wallet) {
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogFileDir());
    }

    public static boolean isDesktopApplicationEnabled() {
        return RuntimeEnvironment.isDesktopApplicationEnabled() && Conch.getBooleanProperty("sharder.launchDesktopApplication");
    }

    private static void launchDesktopApplication() {
        runtimeMode.launchDesktopApplication();
    }

    private Conch() {} // never

    public static Thread fetchUpgradePackageThread(String version) {
        String url = "http://120.79.243.35:8009/sharder-hub/release/cos-hub-" + version +".zip";
        File projectPath = new File("temp/");
        File archive = new File(projectPath, "cos-hub-" + version + ".zip");
        Thread fetchUpgradePackageThread = new Thread(
                () -> {
                    try {
                        if (!archive.exists()) {
                            Logger.logDebugMessage("[UPGRADE CLIENT] Get upgrade package:" + archive.getName());
                            FileUtils.copyURLToFile(new URL(url), archive);
                        }
                        FileUtil.unzipAndReplace(archive, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }

        );
        fetchUpgradePackageThread.setDaemon(true);
        fetchUpgradePackageThread.start();
        return fetchUpgradePackageThread;
    }

    public static void fetchUpgradePackage(String version) throws IOException {
        String url = "http://120.79.243.35:8009/sharder-hub/release/cos-hub-" + version +".zip";
        File projectPath = new File("temp/");
        File archive = new File(projectPath, "cos-hub-" + version + ".zip");
        if (!archive.exists()) {
            Logger.logDebugMessage("[UPGRADE CLIENT] Get upgrade package:" + archive.getName());
            FileUtils.copyURLToFile(new URL(url), archive);
        }
        FileUtil.unzipAndReplace(archive, true);
    }

    // [NAT] init HubConfig or reconfiged restart the application itself
    public static final String SUN_JAVA_COMMAND = "sun.java.command";
    /**
     * Restart the current Java application
     * @param runBeforeRestart some code to be run before restarting
     * @throws IOException
     */
    public static void restartApplication(Runnable runBeforeRestart) {
        try {
            // java binary
            String java = System.getProperty("java.home") + "/bin/java";
            // vm arguments
            List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            StringBuffer vmArgsOneLine = new StringBuffer();
            for (String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in conflict
                if (!arg.contains("-agentlib")) {
                    vmArgsOneLine.append(arg);
                    vmArgsOneLine.append(" ");
                }
            }
            // init the command to execute, add the vm args
            final StringBuffer cmd = new StringBuffer(java + " " + vmArgsOneLine);

            // program main and program arguments
            String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
            // program main is a jar
            if (mainCommand[0].endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                cmd.append("-jar " + new File(mainCommand[0]).getPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                cmd.append("-cp " + System.getProperty("java.class.path") + " " + mainCommand[0]);
            }
            // finally add program arguments
            for (int i = 1; i < mainCommand.length; i++) {
                cmd.append(" ");
                cmd.append(mainCommand[i]);
            }
            // execute the command in a shutdown hook, to be sure that all the
            // resources have been disposed before restarting the application
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        Logger.logDebugMessage(cmd.toString());
                        Runtime.getRuntime().exec(cmd.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            // execute some custom code before restarting
            if (runBeforeRestart!= null) {
                runBeforeRestart.run();
            }
            // exit
            System.exit(0);
        } catch (Exception e) {
            // something went wrong
            e.printStackTrace();
        }
    }

}
