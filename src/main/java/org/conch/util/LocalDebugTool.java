package org.conch.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.conch.account.Account;
import org.conch.env.RuntimeEnvironment;
import org.conch.mint.Generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Used to local debug, contains all debug dirty code
 */
public class LocalDebugTool {

    private static final String LOCAL_DEBUG_CONFIG = "local-debug.properties";
    private static Properties localDebugConfig = new Properties();
    private static final String SEPARATOR = ",";
    static {
        Path confDir = Paths.get(".", "conf");
        Path propPath = Paths.get(confDir.toString()).resolve(Paths.get(LOCAL_DEBUG_CONFIG));
        if (Files.isReadable(propPath)) {
            System.out.printf("Loading %s from dir %s\n", localDebugConfig, confDir);
            try {
                localDebugConfig.load(Files.newInputStream(propPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final boolean isLocalDebugAndBootNodeMode = Generator.isBootNode && isLocalDebug();

    /**
     * used to local debug
     * @return true： local debug mode
     */
    public static boolean isLocalDebug(){
        String localDebugEnv = System.getProperty(RuntimeEnvironment.LOCAL_DEBUG_ARG);
        return StringUtils.isNotEmpty(localDebugEnv) ? Boolean.parseBoolean(localDebugEnv) : false;
    }

    /** debug the poc account cal error **/
    private static final String DEBUG_PROPERTY_POC_ACCOUNTS = "debug.check.poc.accounts";
    private static final List<String> checkPocAccounts = Lists.newArrayList(localDebugConfig.getProperty(DEBUG_PROPERTY_POC_ACCOUNTS,"").split(SEPARATOR));

    public static boolean isCheckPocAccount(String rsAccount){
       return isLocalDebug() && checkPocAccounts.contains(rsAccount);
    }

    public static boolean isCheckPocAccount(long accountId){
       return isLocalDebug() && checkPocAccounts.contains(Account.rsAccount(accountId));
    }

    /** debug the block generator error **/
    private static final String DEBUG_PROPERTY_BLOCK_IDS = "debug.check.block.ids";
    private static final List<String> checkBlockHeights = Lists.newArrayList(localDebugConfig.getProperty(DEBUG_PROPERTY_BLOCK_IDS,"").split(SEPARATOR));

    public static boolean isCheckBlockId(long blockId){
        return isLocalDebug() && checkBlockHeights.contains(String.valueOf(blockId));
    }
}
