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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.conch.account.*;
import org.conch.addons.AddOns;
import org.conch.asset.Asset;
import org.conch.asset.AssetDelete;
import org.conch.asset.AssetDividend;
import org.conch.asset.AssetTransfer;
import org.conch.asset.token.Currency;
import org.conch.asset.token.*;
import org.conch.chain.Blockchain;
import org.conch.chain.BlockchainImpl;
import org.conch.chain.BlockchainProcessor;
import org.conch.chain.BlockchainProcessorImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.PocProcessor;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbBackup;
import org.conch.env.DirProvider;
import org.conch.env.RuntimeEnvironment;
import org.conch.env.RuntimeMode;
import org.conch.env.ServerStatus;
import org.conch.http.API;
import org.conch.http.APIProxy;
import org.conch.http.ForceConverge;
import org.conch.market.*;
import org.conch.mint.CurrencyMint;
import org.conch.mint.Generator;
import org.conch.mint.Hub;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.mq.MessageManager;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.shuffle.Shuffling;
import org.conch.shuffle.ShufflingParticipant;
import org.conch.storage.StorageBackup;
import org.conch.storage.StorageManager;
import org.conch.storage.TaggedData;
import org.conch.storage.tx.StorageTxProcessorImpl;
import org.conch.tools.ClientUpgradeTool;
import org.conch.tx.*;
import org.conch.user.Users;
import org.conch.util.*;
import org.conch.vote.PhasingPoll;
import org.conch.vote.PhasingVote;
import org.conch.vote.Poll;
import org.conch.vote.Vote;
import org.json.simple.JSONObject;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.conch.util.JSON.readJsonFile;

public final class Conch {

    public static final String VERSION = "0.2.0";
    public static final String STAGE = "Alpha";

    public static final com.alibaba.fastjson.JSONObject constantsJsonObj = loadConstantsSettings();
    public static final com.alibaba.fastjson.JSONObject basicConf = (com.alibaba.fastjson.JSONObject) constantsJsonObj.get("basic");
    public static final com.alibaba.fastjson.JSONObject portConf = (com.alibaba.fastjson.JSONObject) constantsJsonObj.get("port");
    public static final com.alibaba.fastjson.JSONObject networkTypeConf = (com.alibaba.fastjson.JSONObject) constantsJsonObj.get("networkType");

    public static final String APPLICATION = basicConf.getString("APPLICATION");
    public static final String COIN_UNIT = basicConf.getString("COIN_UNIT");
    public static final String PROJECT_NAME = basicConf.getString("PROJECT_NAME");

    private static volatile Time time = new Time.EpochTime();

    public static final String CONCH_DEFAULT_PROPERTIES = "sharder-default.properties";
    public static final String CONCH_PROPERTIES = "sharder.properties";
    public static final String CONFIG_DIR = "conf";
    public static String CONCH_CONSTANTS;

    private static final RuntimeMode runtimeMode;
    public static final DirProvider dirProvider;

    private static final Properties DEFAULT_PROPERTIES = new Properties();
    public static final String FOUNDATION_URL = basicConf.getString("FOUNDATION_URL");
    public static final String FOUNDATION_TEST_URL = basicConf.getString("FOUNDATION_TEST_URL");

    public static final Peer.RunningMode runningMode;
    //TODO refactor myAddress, serialNum, nodeIp and nodeType into systemInfo
    private static final String myAddress;
    private static String serialNum = basicConf.getString("SERIAL_NUM");
    private static String nodeType = Peer.Type.NORMAL.getSimpleName();
    public static String nodeIp = IpUtil.getNetworkIp();
    public static Map<Integer, Boolean> airdropHeightMap = Maps.newHashMap();

    public static boolean permissionMode = basicConf.getBooleanValue("PERMISSION_MODE");

    public static boolean isPermissionMode(boolean permissionModeCondition) {
        return Conch.permissionMode && permissionModeCondition;
    }

    /**
     * Load the JSON configuration with respect to Constants
     */
    public static com.alibaba.fastjson.JSONObject loadConstantsSettings() {
        CONCH_CONSTANTS = CONFIG_DIR + "/constants.json";
        return JSON.parseObject(readJsonFile(CONCH_CONSTANTS));
    }

    public static boolean getAirdropHeighStatus(int height) {
        if (airdropHeightMap.get(height) != null) {
            return true;
        }
        return false;
    }

    public static synchronized void setAirdropHeighStatus(int height, boolean status) {
        airdropHeightMap.put(height, status);
    }
    
    public static SystemInfo systemInfo = null;

    public static String getSharderFoundationURL(){
        return Constants.isDevnet() ? FOUNDATION_TEST_URL : FOUNDATION_URL;
    }

    public static String getNetworkType() {
        return Constants.isMainnet() ? networkTypeConf.getString("MAINNET") : Constants.isTestnet() ? networkTypeConf.getString("TESTNET") : networkTypeConf.getString("DEVNET");
    }
    
    public static String getNodeType(){
        CertifiedPeer boundedPeer = Conch.getPocProcessor().getBoundedPeer(Account.rsAccountToId(Generator.getAutoMiningRS()), getHeight());
        if(boundedPeer != null) {
            Conch.nodeType = boundedPeer.getType().getSimpleName();
        }
        
        if(Conch.nodeType == null || Peer.Type.NORMAL.matchSimpleName(Conch.nodeType)){
            // when os isn't windows and mac, it should be hub/box or server node
            if (!SystemUtils.IS_OS_WINDOWS
                    && !SystemUtils.IS_OS_MAC
                    && StringUtils.isNotEmpty(getSerialNum())) {

                try {
                    Conch.nodeType = getTypeSimpleName(getSerialNum());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return Conch.nodeType;
    }

    /**
     * get simple name according to serial num, default type is node if there is no num exist.
     *
     * @param num serial number
     * @return Peer Type Simple Name
     * @throws IOException
     */
    private static String getTypeSimpleName(String num) throws IOException {

        if (StringUtils.isEmpty(num)) {
            return Peer.Type.NORMAL.getSimpleName();
        }

        String url = UrlManager.getFoundationUrl(
                UrlManager.GET_HARDWARE_TYPE_EOLINKER,
                UrlManager.GET_HARDWARE_TYPE_LOCAL,
                UrlManager.GET_HARDWARE_TYPE_PATH
        );

        RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(url)
                .get()
                .addPathParam("serialNum", num.replaceAll("(\\r\\n|\\n)", ""))
                .request();
        com.alibaba.fastjson.JSONObject result = JSON.parseObject(response.getContent());

        Integer nodeTypeCode = Peer.Type.SOUL.getSimpleCode();
        if (result.getBoolean(Constants.SUCCESS)) {
            com.alibaba.fastjson.JSONObject data = result.getJSONObject("data");
            if (data == null || data.getInteger("type") == null) {
                return Peer.Type.NORMAL.getSimpleName();
            }
            nodeTypeCode = data.getInteger("type");
        } else {
            Logger.logWarningMessage(String.format("failed to get node type by serial number[%s]!", num));
        }

        return Peer.Type.getSimpleName(nodeTypeCode);
    }

    private static int readSerialNoCount = 0;
    public static String getSerialNum(){
        if((StringUtils.isEmpty(Conch.serialNum) || Conch.serialNum.length() < 6)
        && readSerialNoCount == 0) {
            readAndSetSerialNum();
        }
        // every specified times to read the serial no from
        if(readSerialNoCount++ == 100){
            readSerialNoCount = 0;
        }
        return Conch.serialNum;
    }
    
    public static boolean hasSerialNum(){
       return StringUtils.isNotEmpty(getSerialNum()) && getSerialNum().length() > 5;
    }

    private static void readAndSetSerialNum(){
        String filePath = ".hubSetting/.tempCache/.sysCache";
        String userHome = Paths.get(System.getProperty("user.home"), filePath).toString();
        File tempFile = new File(userHome);
        // node check if serial number exist
        if (tempFile.exists()) {
            String num = null;
            try {
                num = FileUtils.readFileToString(tempFile, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Conch.serialNum = StringUtils.isEmpty(num) ? "" : num.replaceAll("(\\r\\n|\\n)", "");
            if(Logger.printNow(Logger.CONCH_P_readAndSetSerialNum)) {
                Logger.logDebugMessage("serialNum => " + Conch.serialNum);
            }
        }
    }
    
    /**
     * Preset parameters
     */
    public static class PresetParam {
        public static final int DEFAULT_PEER_PORT = portConf.getIntValue("DEFAULT_PEER_PORT");
        public static final int DEFAULT_API_PORT = portConf.getIntValue("DEFAULT_API_PORT");
        public static final int DEFAULT_API_SSL_PORT = portConf.getIntValue("DEFAULT_API_SSL_PORT");

        public Constants.Network network;
        public int peerPort;
        public int apiPort;
        public int apiSSLPort;

        public PresetParam(Constants.Network network, int peerPort, int apiPort, int apiSSLPort) {
            this.network = network;
            this.peerPort = peerPort;
            this.apiPort = apiPort;
            this.apiSSLPort = apiSSLPort;
        }

        private static Map<Constants.Network,PresetParam> presetMap = new ConcurrentHashMap<>();
        static {
            //preset params
            presetMap.clear();
            presetMap.put(Constants.Network.DEVNET, new PresetParam(Constants.Network.DEVNET, portConf.getIntValue("DEVNET_PEER"), portConf.getIntValue("DEVNET_API"), portConf.getIntValue("DEVNET_API_SSL")));
            presetMap.put(Constants.Network.TESTNET, new PresetParam(Constants.Network.TESTNET, portConf.getIntValue("TESTNET_PEER"), portConf.getIntValue("TESTNET_API"), portConf.getIntValue("TESTNET_API_SSL")));
            presetMap.put(Constants.Network.MAINNET, new PresetParam(Constants.Network.MAINNET, portConf.getIntValue("MAINNET_PEER"), portConf.getIntValue("MAINNET_API"), portConf.getIntValue("MAINNET_API_SSL")));
        }

        public static void print(){
            if(presetMap == null || presetMap.size() == 0) {
                System.out.println("preset param map is null, nothing is preset!");
            }
        }

        public static int getPeerPort(Constants.Network network){
            PresetParam presetParam = presetMap.get(network);
            return presetParam != null ?  presetParam.peerPort : DEFAULT_PEER_PORT;
        }

        public static int getApiPort(Constants.Network network){
            PresetParam presetParam = presetMap.get(network);
            return presetParam != null ?  presetParam.apiPort : DEFAULT_API_PORT;
        }

        public static int getApiSSLPort(Constants.Network network){
            PresetParam presetParam = presetMap.get(network);
            return presetParam != null ?  presetParam.apiSSLPort : DEFAULT_API_SSL_PORT;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static int getPeerPort(){
        return PresetParam.getPeerPort(Constants.getNetwork());
    }

    public static int getApiPort(){
        return PresetParam.getApiPort(Constants.getNetwork());
    }

    public static int getApiSSLPort(){
        return PresetParam.getApiSSLPort(Constants.getNetwork());
    }


    static {
        redirectSystemStreams("out");
        redirectSystemStreams("err");
        System.out.println("Initializing COS server version " + Conch.VERSION);
        printCommandLineArguments();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        System.out.printf("Runtime mode %s\n", runtimeMode.getClass().getName());
        dirProvider = RuntimeEnvironment.getDirProvider();
        System.out.println("User home folder " + dirProvider.getUserHomeDir());
        loadProperties(DEFAULT_PROPERTIES, CONCH_DEFAULT_PROPERTIES, true);
        runningMode = getRunningMode();
        PresetParam.print();
    }


    public static String getMyAddress(){
        return myAddress;
    }
    
    
//    public static boolean address(String newAnnouncedAddress){
//        URI uri = new URI("http://" + newAnnouncedAddress);
//        int announcedPort = uri.getPort() == -1 ? Conch.getPeerPort() : uri.getPort();
//        InetAddress address = InetAddress.getByName(Peers.addressHost(host));
//        for (InetAddress inetAddress : InetAddress.getAllByName(uri.getHost())) {
//            if (inetAddress.equals(address)) {
//                return true;
//            }
//        }
//    }
    public static boolean matchMyAddress(String host){
        try{
            if(StringUtils.isEmpty(myAddress)){
                return false;
            }

            if(StringUtils.isEmpty(host)) {
                return false;
            }

            if(Conch.useNATService) {
                return myAddress.equalsIgnoreCase(host);
            }

            if(IpUtil.isDomain(host)) {
                if(myAddress.equalsIgnoreCase(host)) {
                    return true;
                }
                return IpUtil.getHost(myAddress).equalsIgnoreCase(IpUtil.getHost(host));
            } 
        } catch(Exception e){
            Logger.logErrorMessage("can't finish matchMyAddress with host[" + host + "] caused by " + e.getMessage());
        }
        return false;
        // FIXME to improve the security, need check the public network ip of current node
//        if(StringUtils.isEmpty(nodeIp)) nodeIp = IpUtil.getNetworkIp();
//        return nodeIp.equalsIgnoreCase(IpUtil.getIpFromUrl(host));
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

    private static final Properties properties = new Properties(DEFAULT_PROPERTIES);

    static {
        loadProperties(properties, CONCH_PROPERTIES, false);
        
        // use the external ip as its myAddress default value
        myAddress = readAndParseMyAddress();

        // check port of myAddress whether equal to port of TESTNET
        if (myAddress != null && myAddress.endsWith(":" + PresetParam.getPeerPort(Constants.Network.TESTNET)) && !Constants.isTestnet()) {
            throw new RuntimeException("Port " + PresetParam.getPeerPort(Constants.Network.TESTNET) + " should only be used for testnet!!!");
        }
    }

    private static String readAndParseMyAddress(){
        String myAddr = Convert.emptyToNull(Conch.getStringProperty("sharder.myAddress", "").trim());
        boolean closeAutoSwitchIp = Conch.getBooleanProperty("sharder.closeAutoSwitchIp");
        // correct the undefined issue of myAddress
        if("undefined".equalsIgnoreCase(myAddr)){
            myAddr = IpUtil.getNetworkIp().trim();
            Conch.storePropertieToFile("sharder.myAddress", myAddr);
        }

        if(closeAutoSwitchIp) {
            Logger.logInfoMessage("Auto check and switch the internal ip to public ip is CLOSED");
        }else{
            // correct the internal IP to public IP if the client have the public IP at the every client start
            if(StringUtils.isEmpty(myAddr)
            || (!IpUtil.isDomain(myAddr) && IpUtil.isInternalIp(myAddr)) // myAddress is not the domain and it is internal ip
            ) {
                myAddr = IpUtil.getNetworkIp().trim();
                Conch.storePropertieToFile("sharder.myAddress", myAddr);
            }
        }

        return  myAddr;
    }
    /**
     * [NAT] useNATService and client configuration
     */
    private static boolean useNATService = Conch.getBooleanProperty("sharder.useNATService");
    public static final String NAT_SERVICE_ADDRESS = Convert.emptyToNull(Conch.getStringProperty("sharder.NATServiceAddress"));
    public static final int NAT_SERVICE_PORT = Conch.getIntProperty("sharder.NATServicePort");
    static final String NAT_CLIENT_KEY = Convert.emptyToNull(Conch.getStringProperty("sharder.NATClientKey"));

    public static boolean isUseNAT(){
        return useNATService;
    }

    static {
        
        try {
            if (isUseNAT()) {
                Logger.logInfoMessage("[NAT] Node joins the network via foundation NAT or 3rd part NAT|DDNS service");
                
                File natCmdFile = new File(SystemUtils.IS_OS_WINDOWS ? "nat_client.exe" : "nat_client");

                if(natCmdFile.exists()){
                    StringBuilder cmd = new StringBuilder(SystemUtils.IS_OS_WINDOWS ? "cmd /c nat_client.exe" : "./nat_client");
                    cmd.append(" -s ").append(NAT_SERVICE_ADDRESS == null ? addressHost(myAddress) : NAT_SERVICE_ADDRESS)
                            .append(" -p ").append(NAT_SERVICE_PORT)
                            .append(" -k ").append(NAT_CLIENT_KEY);
                    // nat log file
                    if(SystemUtils.IS_OS_WINDOWS ) {
                        cmd.append(" >> ./logs/nat.log");
                    }else {
                        cmd.append(" > ./logs/nat.log 2>&1");
                    }
                    
                    Process process = Runtime.getRuntime().exec(cmd.toString());
                    // any error message?
                    StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
                    // any output?
                    StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");
                    // kick them off
                    errorGobbler.start();
                    outputGobbler.start();

                    if(SystemUtils.IS_OS_UNIX) {
                        Process natProcess = Runtime.getRuntime().exec("find /etc/init.d/ -name net_client");
                        InputStreamReader isr = new InputStreamReader(natProcess.getInputStream());
                        BufferedReader br = new BufferedReader(isr);
                        if (br.readLine() == null){
                            Logger.logInfoMessage("set net_client auto start");
                            //use the installation folder of Sharder as execution path
                            Process autoStart = Runtime.getRuntime().exec("cp " + Conch.getUserHomeDir() + " /etc/init.d");
                            Runtime.getRuntime().addShutdownHook(new Thread(() -> autoStart.destroy()));
                        }
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> natProcess.destroy()));
                    }else if(SystemUtils.IS_OS_WINDOWS){
                        //TODO windows support, set the as a msc.service
                    }
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroy()));
                    Logger.logInfoMessage("[NAT] NAT client executed: " + cmd.toString());
                }else{
                    Logger.logWarningMessage("[NAT] useNatService is true but command file not exist");
                }
            }else{
                Logger.logInfoMessage("[NAT] NAT service be set to close");
            }
        } catch (Exception e) {
            useNATService = false;
            Logger.logErrorMessage("[NAT] NAT Client execute Error", e);
        }

    }

    public static int addressPort(String address) {
        return Optional.ofNullable(address).map(Conch::addressURI).map(URI::getPort).orElse(0);
    }

    public static String addressHost(String address) {
        return Optional.ofNullable(address).map(Conch::addressURI).map(URI::getHost).orElse(null);
    }

    static URI addressURI(String address) {
        try {
            return new URI("http://" + address);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static void storePropertieToFile(String k, String v) {
        HashMap<String, String> parameters = Maps.newHashMap();
        parameters.put(k, v);
        storePropertiesToFile(parameters);
    }
    
    public static void storePropertiesToFile(HashMap<String, String> parameters) {
        OutputStream output = null;
        Properties userProperties = loadProperties(properties, CONCH_PROPERTIES, false);
        parameters.entrySet().forEach(map -> userProperties.setProperty(map.getKey(), map.getValue()));
        try {
            output = new FileOutputStream("conf/" + CONCH_PROPERTIES);
            LocalDateTime now = LocalDateTime.now();
            userProperties.store(output , "Updated at " + now.toString());
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
            Logger.logMessage(name + " not defined, using default value = \"" + (doNotLog ? "{not logged}" : defaultValue) + "\"");
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
        return getBooleanProperty(name, false);
    }

    public static Boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);

        if(StringUtils.isEmpty(value)) {
            Logger.logMessage(name + " not defined, use the default value " + defaultValue);
            return  defaultValue;
        }
        else if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        }
        else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        return false;
    }

    public static boolean containProperty(String name){
        String value = properties.getProperty(name);
        return StringUtils.isNotEmpty(value);
    }

    public static int getHeight(){
        return getBlockchain().getHeight();
    }
    
    public static PocProcessor getPocProcessor(){
        return PocProcessorImpl.instance;
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
        return new TransactionImpl.BuilderImpl(senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment)attachment);
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
     * @return current time - beginning time (unit is second)
     */
    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Conch.time = time;
    }

//    @EnableSwagger2Doc
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
        SharderPoolProcessor.persistence();
        API.shutdown();
        Users.shutdown();
        FundingMonitor.shutdown();
        ThreadPool.shutdown();
        BlockchainProcessorImpl.getInstance().shutdown();
        Peers.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("COS server " + getFullVersion() + " "  + getCosUpgradeDate() + " stopped.");
        Logger.shutdown();
        runtimeMode.shutdown();
    }

    private static class Init {

        static volatile boolean initialized = false;

        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                setSystemProperties();
                logSystemProperties();
                runtimeMode.init();
                Thread secureRandomInitThread = initSecureRandom();
                setHeartBeatTimer();
                ForceConverge.init();
                setServerStatus(ServerStatus.BEFORE_DATABASE, null);
//                CompactDatabase.checkAndRestore();
                try {
                    Db.init();
                }catch(Exception e){
                    Logger.logErrorMessage("[DB INIT EXCEPTION] Can't init the  db instance", e);
//                    Logger.logWarningMessage("[DB EXCEPTION HANDLE] Fetch and restore to last db archive because the db instance init failed[ %s ]", e.getMessage());
//                    ClientUpgradeTool.forceDownloadFromOSS = true;
//                    ClientUpgradeTool.restoreDbToLastArchive(true, true);
//                    ClientUpgradeTool.forceDownloadFromOSS = false;
                }

                setServerStatus(ServerStatus.AFTER_DATABASE, null);
                StorageManager.init();

                SharderPoolProcessor.init();
                PocProcessorImpl.init();

                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();

                Account.init();
                AccountRestrictions.init();
                AccountLedger.init();
                Alias.init();

                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                PhasingPoll.init();
                Trade.init();

                Asset.init();
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
                StorageTxProcessorImpl.init();
                StorageBackup.init();
                FxtDistribution.init();
                Peers.init();
                MessageManager.init();
                APIProxy.init();
                Generator.init();
                AddOns.init();
                API.init();
                Users.init();
                DebugTrace.init();
                DbBackup.init();

//                Account.truncateHistoryData();

                int timeMultiplier = (Constants.isTestnetOrDevnet() && Constants.isOffline) ? Math.max(Conch.getIntProperty("sharder.timeMultiplier"), 1) : 1;
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
                Logger.logMessage("COS server " + getFullVersion() + " " + getCosUpgradeDate() + " started successfully");
                Logger.logMessage("Copyright © 2019 " + Conch.FOUNDATION_URL);
                Logger.logMessage("Distributed under MIT");
                if (API.getWelcomePageUri() != null) {
                    Logger.logMessage("Client UI URL is " + API.getWelcomePageUri());
                }

                setServerStatus(ServerStatus.STARTED, API.getWelcomePageUri());

                if (isDesktopMode()) {
                    runtimeMode.launchDesktopApplication();
                }

                if (Constants.isTestnetOrDevnet()) {
                    Logger.logMessage("RUNNING ON " +  Constants.getNetwork()  + " - DO NOT USE MAINNET ACCOUNTS!");
                }

                Peers.sysInitialed = true;
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                runtimeMode.alert(e.getMessage() + "\n" +
                        "See additional information in " + dirProvider.getLogFileDir() + System.getProperty("file.separator") + "sharder.log");
                // Don't exit when initial the Conch instance occur exception. 
                // Because the ForceConverge.autoUpgrade will fix the error according to upgrade the COS version
                
//                System.exit(1);
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
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
                RuntimeEnvironment.DIRPROVIDER_ARG,
                RuntimeEnvironment.NETWORK_ARG,
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
    
    public static boolean isInitialized(){
        return Init.initialized;
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

    public static boolean isDesktopMode() {
        return Peer.RunningMode.DESKTOP.equals(runningMode);
    }

    /**
     * running mode
     *
     * @return
     */
    private static Peer.RunningMode getRunningMode(){
        Peer.RunningMode mode = Peer.RunningMode.OTHERS;
        // server node should not running on the windows and mac os
        if (!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC) {
            Path serialFile = Paths.get(System.getProperty("user.home")).resolve(".hubSetting/.tempCache/.sysCache");
            if(Files.exists(serialFile)){
                mode = Peer.RunningMode.COMMAND;
            }else if(RuntimeEnvironment.isDesktopApplicationEnabled()
                    && Conch.getBooleanProperty("sharder.launchDesktopApplication")) {
                mode = Peer.RunningMode.DESKTOP;
            }
        }else {
            if(!RuntimeEnvironment.isDesktopApplicationEnabled()
                    || !Conch.getBooleanProperty("sharder.launchDesktopApplication")) {
                mode = Peer.RunningMode.COMMAND;
            }
        }
        return mode;
    }

    public static boolean reachLastKnownBlock(){
        if(Constants.isDevnet() && Generator.isBootNode) {
            return true;
        }
        int height = Conch.getHeight();
        if (height < Constants.LAST_KNOWN_BLOCK) {
            if(Logger.printNow(Logger.CONCH_P_reachLastKnownBlock)) {
                Logger.logDebugMessage("current height %d is less than last known height %s and current state is %s, wait till blocks sync finished..."
                        , height, Constants.LAST_KNOWN_BLOCK, Peers.getMyBlockchainStateName());
            }
            return false;
        }
        return true;
    }

    private Conch() {} // never


    // [NAT] init HubConfig or reconfiged restart the application itself
    public static final String SUN_JAVA_COMMAND = "sun.java.command";
    /**
     * Restart the current Java application
     * @param runBeforeRestart some code to be run before restarting
     * @throws IOException
     */
    public static void restartApplication(Runnable runBeforeRestart) {
        try {
            pause();


            //            Logger.logInfoMessage("Clear the all logs");
            //            FileUtil.clearAllLogs();


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
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdown();
                    Logger.logDebugMessage(cmd.toString());
                    Runtime.getRuntime().exec(cmd.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            // execute some custom code before restarting
            if (runBeforeRestart!= null) {
                runBeforeRestart.run();
            }
            // exit
            Logger.logInfoMessage("COS Server Shutting down...");
            System.exit(0);
        } catch (Exception e) {
            // something went wrong
            e.printStackTrace();
            Logger.logErrorMessage("restart application error",e);
        } finally {
            unpause();
        }
    }
    
    public static boolean pause(){
        try{
            getBlockchainProcessor().setGetMoreBlocks(false);
            Generator.pause(true); 
        }catch(Exception e){
            Logger.logErrorMessage("pause failed",e);
            return false;
        }
        return true;
    }

    public static boolean unpause(){
        try{
            Conch.getBlockchainProcessor().setGetMoreBlocks(true);
            Generator.pause(false);
        }catch(Exception e){
            Logger.logErrorMessage("unpause failed",e);
            return false;
        }
        return true;
    }
    
    
    /**
     * 
     * @param paramMap properties need be reset before reboot
     * @param restart
     * @return 
     */
    public static boolean resetAndReboot(HashMap<String, String> paramMap, boolean restart){
        try {
            Conch.pause();
            
            if(paramMap == null) {
                paramMap = Maps.newHashMap();
            }
            // delete the local db
            paramMap.put(ForceConverge.PROPERTY_MANUAL_RESET, "true");
            Conch.storePropertiesToFile(paramMap);

            // delete log files
            FileUtil.clearAllLogs();

            if (restart) {
                new Thread(() -> Conch.restartApplication(null)).start();
            }
            
        } catch (Exception e) {
            Logger.logErrorMessage("reset settings and reboot failed",e);
        } finally {
            Conch.unpause();
        }
        return true;
    }

    /**
     * Version compare
     * @param version compared version
     * @return -1 : Conch.version < version; 
     *         0 : Conch.version = version; 
     *         1 : Conch.version > version
     */
    public static int versionCompare(String version){
        if(StringUtils.isEmpty(version)) {
            return -1;
        }
        
        Integer verInt = Integer.valueOf(version.replaceAll("\\.", ""));
        Integer currentVerInt = Integer.valueOf(VERSION.replaceAll("\\.", ""));

        return currentVerInt.compareTo(verInt);
    }

    /**
     * -1: compared version is a new version
     * 0: compared version is a same version
     * 1: compared version is a old version
     * @param version compared version, format is x.x.x
     * @param build build time, format is yyyy-MM-dd HH:mm:ss
     * @return  -1 : Conch.version < version or Conch.version = version and build time is earlier than the compared version; 
     *          0 : Conch.version = version and build time is same with the compared version; 
     *          1 : Conch.version > version or Conch.version = version and build time is later than the compared version; 
     */
    public static int versionCompare(String version,String build){
        try {
            if(versionCompare(version) == -1){
                return -1;
            }else if(Conch.versionCompare(version) == 0){
                Date currentBuild = _convertUpdateDate(ClientUpgradeTool.cosLastUpdateDate);
                Date ossBuild = _convertUpdateDate(build);
                
                if(ossBuild == null) {
                    return 1;
                }
                if(currentBuild == null) {
                    return -1;
                }
                if(currentBuild.before(ossBuild)) {
                    return -1;
                }
                if(currentBuild.after(ossBuild)) {
                    return 1;
                }
                if(ossBuild.getTime() == currentBuild.getTime()) {
                    return 0;
                }
            }else if(Conch.versionCompare(version) == 1) {
                return 1;
            }
        } catch (Exception e) {
            Logger.logErrorMessage("versionCompare occur unknown exception", e);
        }
        return 0;
    }


    /**
     * - check the last cos version on the OSS
     * - auto upgrade at the new version be found
     */
    static final String UPDATE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    static final String UPDATE_DATE_FORMAT_SHORT = "yyyy-MM-dd HH:mm";

    private static Date _convertUpdateDate(String dateStr) throws ParseException {
        if(StringUtils.isEmpty(dateStr)) {
            return null;
        }

        // short date format 'yyyyy-MM-dd HH:mm'
        if(dateStr.length() == 16) {
            return DateUtils.parseDate(dateStr,UPDATE_DATE_FORMAT_SHORT);
        }

        // long date format 'yyyyy-MM-dd HH:mm:ss'
        if(dateStr.length() == 19) {
            return DateUtils.parseDate(dateStr,UPDATE_DATE_FORMAT);
        }

        return null;
    }
    
    
    
    /**
     * Full version format is : version number - stage
     * e.g. 0.0.1-Beta or 0.0.1-Alpha
     * @return 
     */
    public static String getFullVersion(){
        return VERSION + "-" + STAGE;
    }
    public static String getVersionWithBuild(){
        return VERSION + "-" + STAGE + " " + ClientUpgradeTool.cosLastUpdateDate;
    }
    public static String getVersion(){ return VERSION; }
    public static String getCosUpgradeDate(){ return ClientUpgradeTool.cosLastUpdateDate; }

    public static void setHeartBeatTimer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Logger.logDebugMessage("[HeartBeat] COS is working properly");
            }
        }, 3*60*1000, Constants.HeartBeat_Time);
    }

}
