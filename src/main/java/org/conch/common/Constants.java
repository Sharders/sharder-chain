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

package org.conch.common;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.chain.BlockchainProcessorImpl;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.conch.env.RuntimeEnvironment;
import org.conch.mint.Generator;
import org.conch.peer.Peer;

import java.util.*;

public final class Constants {

    /**
     * Network definition
     */
    public enum Network {
        /**
         * online
         */
        MAINNET("Mainnet"),
        /**
         * test environment
         */
        TESTNET("Testnet"),
        DEVNET("Devnet");

        private final String name;

        /** Private constructor so it cannot be instantiated */
        Network(String name) {
            this.name = name;
        }

        public static <T extends Enum<T>> T valueOfIgnoreCase(Class<T> enumeration, String name) {

            for (T enumValue : enumeration.getEnumConstants()) {
                if (enumValue.name().equalsIgnoreCase(name)) {
                    return enumValue;
                }
            }

            throw new IllegalArgumentException(
                    String.format("There is no value with name '%s' in Enum %s, please add the conf folder into runtime firstly!", name, enumeration.getName()));
        }

        public String getName() {
            return name;
        }

        public boolean is(String name){
            return this.name.equalsIgnoreCase(name);
        }

        public static Network get(String name) {
            for(Network  network :  values()) {
                if(network.name.equalsIgnoreCase(name)){
                    return network;
                }
            }
            return null;
        }
    }

    private static final String networkInProperties = Conch.getStringProperty("sharder.network");
    public static final String NetworkDef = loadNetworkDefinition();
    public static final boolean isOffline = Conch.getBooleanProperty("sharder.isOffline");
    public static final boolean isLightClient = Conch.getBooleanProperty("sharder.isLightClient");
    public static final boolean isStorageClient = Conch.getBooleanProperty("sharder.enableStorage");
    public static final List<String> bootNodesHost = parseBootNodesHost();
    public static final String bootNodeHost = parseBootNodeHost();

//    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 5000;
    public static final int MIN_TRANSACTION_SIZE = 176;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * MIN_TRANSACTION_SIZE * 244;
    public static final long MAX_BALANCE_SS = 1000000000;
    public static final long ONE_SS = 100000000;
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_SS * ONE_SS;
    
    /** another initial env => target: 6000, min-limit: 17, max-limit=22, base-gamma: 21 */
    public static final long INITIAL_BASE_TARGET = isTestnetOrDevnet() ? (153722867 * 67) : (153722867 * 8);
    public static final int MIN_BLOCKTIME_LIMIT = 53;
    public static final int MAX_BLOCKTIME_LIMIT = 67;
    public static final int BASE_TARGET_GAMMA = 64;

    public static final int MAX_ROLLBACK = Math.max(Conch.getIntProperty("sharder.maxRollback"), 720);
    public static final long MAX_BASE_TARGET = MAX_BALANCE_SS * INITIAL_BASE_TARGET;
    public static final long MAX_BASE_TARGET_2 = isTestnetOrDevnet() ? MAX_BASE_TARGET : INITIAL_BASE_TARGET * 50;
    public static final long MIN_BASE_TARGET = INITIAL_BASE_TARGET * 9 / 10;

    /** for the security, you can set the confirmations = 1440 */
    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = isDevnet() ? 1 :(isTestnet()? 3 : 12);
    public static final int LEASING_DELAY = isTestnetOrDevnet() ? Conch.getIntProperty("sharder.testnetLeasingDelay", 10) : 205;
    public static final long MIN_FORGING_BALANCE_NQT = 1000 * ONE_SS;

    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference
    public static final int MINING_DELAY = Conch.getIntProperty("sharder.miningDelay");
    public static final int MINING_SPEEDUP = Conch.getIntProperty("sharder.miningSpeedup");

    public static final byte MAX_PHASING_VOTE_TRANSACTIONS = 10;
    public static final byte MAX_PHASING_WHITELIST_SIZE = 10;
    public static final byte MAX_PHASING_LINKED_TRANSACTIONS = 10;
    public static final int MAX_PHASING_DURATION = 14 * 1440;
    public static final int MAX_PHASING_REVEALED_SECRET_LENGTH = 100;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 160;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 160 + 16;

    public static final int MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024;
    public static final int MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH = 42 * 1024;

    public static final int MIN_PRUNABLE_LIFETIME = isTestnetOrDevnet() ? 1440 * 60 : 14 * 1440 * 60;
    public static final int MAX_PRUNABLE_LIFETIME;
    public static final boolean ENABLE_PRUNING;
    static {
        int maxPrunableLifetime = Conch.getIntProperty("sharder.maxPrunableLifetime");
        ENABLE_PRUNING = maxPrunableLifetime >= 0;
        MAX_PRUNABLE_LIFETIME = ENABLE_PRUNING ? Math.max(maxPrunableLifetime, MIN_PRUNABLE_LIFETIME) : Integer.MAX_VALUE;
    }
    public static final boolean INCLUDE_EXPIRED_PRUNABLE = Conch.getBooleanProperty("sharder.includeExpiredPrunable");

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final int MAX_ACCOUNT_PROPERTY_NAME_LENGTH = 32;
    public static final int MAX_ACCOUNT_PROPERTY_VALUE_LENGTH = 160;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH = 160;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;
    public static final int MAX_DIVIDEND_PAYMENT_ROLLBACK = 1441;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;
    public static final int MAX_POLL_DURATION = 14 * 1440;

    public static final byte MIN_VOTE_VALUE = -92;
    public static final byte MAX_VOTE_VALUE = 92;
    public static final byte NO_VOTE_VALUE = Byte.MIN_VALUE;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 1000;

    public static final int MAX_HUB_ANNOUNCEMENT_URIS = 100;
    public static final int MAX_HUB_ANNOUNCEMENT_URI_LENGTH = 1000;
    public static final long MIN_HUB_EFFECTIVE_BALANCE = 100000;

    public static final int MIN_CURRENCY_NAME_LENGTH = 3;
    public static final int MAX_CURRENCY_NAME_LENGTH = 10;
    public static final int MIN_CURRENCY_CODE_LENGTH = 3;
    public static final int MAX_CURRENCY_CODE_LENGTH = 5;
    public static final int MAX_CURRENCY_DESCRIPTION_LENGTH = 1000;
    public static final long MAX_CURRENCY_TOTAL_SUPPLY = 1000000000L * 100000000L;
    public static final int MAX_MINTING_RATIO = 10000; // per mint units not more than 0.01% of total supply
    public static final byte MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS = 3;
    public static final byte MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS = 30; // max possible at current block payload limit is 51
    public static final short MAX_SHUFFLING_REGISTRATION_PERIOD = (short)1440 * 7;
    public static final short SHUFFLING_PROCESSING_DEADLINE = (short) (isTestnetOrDevnet() ? 10 : 100);

    public static final int MAX_TAGGED_DATA_NAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGGED_DATA_TAGS_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_CHANNEL_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_FILENAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DATA_LENGTH = 10 * 1024 * 1024;


    public static final int MAX_STORED_DATA_NAME_LENGTH = 200;
    public static final int MAX_STORED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_STORED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_STORED_DATA_CHANNEL_LENGTH = 100;
    public static final int MIN_EXISTENCE_HEIGHT = 100;

    public static final int POC_BLOCK_HEIGHT = 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK = 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP = 0;

    public static final int FXT_BLOCK = isTestnetOrDevnet() ? 10000 : 10000; 
    
    public static final int LAST_KNOWN_BLOCK = isDevnet() ?  1 : (isTestnet() ? 268 : 500);
    
    public static final int TESTNET_PHASE_ONE = 500000;
    public static final int TESTNET_PHASE_TWO = 990000;
    public static final String TESTNET_PHASE_ONE_TIME = "2019-06-30 00:00:00";
    public static final String TESTNET_PHASE_TWO_TIME = "2019-09-30 00:00:00";
    public static final int POC_LEDGER_RESET_HEIGHT = isTestnet() ? 4500 : 0;
    public static final int POC_NEW_ALGO_HEIGHT = isTestnet() ? 4751 : 0;
    public static final int POC_SS_HELD_SCORE_PHASE1_HEIGHT = isTestnet() ? 4765 : 0;
    public static final int POC_SS_HELD_SCORE_PHASE2_HEIGHT = isTestnet() ? 13777 : 0;
    public static final int POC_POOL_NEVER_END_HEIGHT = isTestnet() ? 13777 : 0;
    public static final int POC_BALANCE_CORRECTION_HEIGHT = isTestnet() ? 15777 : 0;

    //not opened yet
    public static final int PHASING_BLOCK_HEIGHT = Integer.MAX_VALUE;
    public static final int DIGITAL_GOODS_STORE_BLOCK = Integer.MAX_VALUE;
    public static final int TRANSPARENT_FORGING_BLOCK_HUB_ANNOUNCEMENT = Integer.MAX_VALUE;
    public static final int MONETARY_SYSTEM_BLOCK = Integer.MAX_VALUE;

    public static final int SHUFFLING_BLOCK_HEIGHT = isTestnetOrDevnet() ? 0 : 0;
  
    public static final int MAX_REFERENCED_TRANSACTION_TIMESPAN = 60 * 1440 * 60;

    public static final int[] MIN_VERSION = new int[] {0, 0, 1};
    public static final int[] MIN_PROXY_VERSION = new int[] {0, 0, 1};

    public static final long UNCONFIRMED_POOL_DEPOSIT_NQT = (isTestnetOrDevnet() ? 50 : 100) * ONE_SS;
    public static final long SHUFFLING_DEPOSIT_NQT = (isTestnetOrDevnet() ? 7 : 1000) * ONE_SS;

    public static final boolean correctInvalidFees = Conch.getBooleanProperty("sharder.correctInvalidFees");
    public static final String ACCOUNT_PREFIX = "CDW-"; //account prefix，SSA: Sharder Storage Account

    //chain begin time
    public static final long EPOCH_BEGINNING = launchedTime(0).getTimeInMillis();

    //Mining pool
    public static final int SHARDER_POOL_DELAY = isDevnet() ? 1 : 1; //transaction become effective
    public static final int SHARDER_POOL_MAX_BLOCK_DESTROY = 5; //pool can be destroyed by manual
    public static final int SHARDER_POOL_DEADLINE = isDevnet() ? 60 * 24 : 60 * 24 * 7; 
    public static final int SHARDER_REWARD_DELAY = isDevnet() ? 1 : (isTestnet() ? 3 : 7);
    public static final int SHARDER_POOL_JOIN_TX_VALIDATION_HEIGHT = isDevnet() ? 1 : (isTestnet() ? 300 : 1);

    //Coinbase
    public static final int MAX_COINBASE_TYPE_LENGTH = 16;
    
    /**
     * chain begin time
     * @param index 0: conch chain, 1: testnet of sharder, otherwise is mainnet of sharder
     * @return
     */
    static Calendar launchedTime(int index){
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int year = index == 0 ? 2016 : (index == 1 ? 2019 : 2019);
        int month = index == 0 ? Calendar.AUGUST : (index == 1 ? Calendar.JANUARY : Calendar.SEPTEMBER);
        int day = index == 0 ? 16 : (index == 1 ? 1 : 9);
        int hms = index == 0 ? 8 : (index == 1 ? 1 : 9);
        
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hms);
        calendar.set(Calendar.MINUTE, hms);
        calendar.set(Calendar.SECOND, hms);
        return calendar;
    }

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Constants() {} // never

    //Fee
    public static final ArrayList<Long> configFee = new ArrayList<>();

    static {
        configFee.add((long)Conch.getIntProperty("sharder.fee.payment"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.message"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.digitalGoods"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.accountControl"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.monetarySystem"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.dataFee"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.shuffling"));
        //TODO modify storage fee config
        long fee = (long)Conch.getIntProperty("sharder.fee.data0M");
        configFee.add(fee == 0 ? Constants.ONE_SS : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data1M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data2M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data3M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data4M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data5M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data6M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data7M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data8M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data9M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
        fee = (long)Conch.getIntProperty("sharder.fee.data10M");
        configFee.add(fee == 0 ? configFee.get(configFee.size() - 1) : fee);
    }

    // Network
    public static final boolean isMainnet() {
        return Network.MAINNET.is(NetworkDef);
    }

    public static final boolean isTestnet() {
        return Network.TESTNET.is(NetworkDef);
    }

    public static final boolean isDevnet() {
        return Network.DEVNET.is(NetworkDef);
    }

    public static final boolean isTestnetOrDevnet() {
        return isTestnet() || isDevnet();
    }

    public static Network getNetwork(){
        return Network.valueOfIgnoreCase(Network.class,NetworkDef);
    }
    
    //default gap of mainnet & testnet is 7 min
    private static final int blockGapInProperties = isDevnet() ?  Conch.getIntProperty("sharder.devnetBlockGap") : 
            ( isTestnet() ? Conch.getIntProperty("sharder.testnetBlockGap", 7) : Conch.getIntProperty("sharder.blockGap", 7));

    /**
     * interval between two block generation, the min is 1min
     * @return generation gap in seconds
     */
    public static int getBlockGapSeconds(){
        int gap = blockGapInProperties > 1 ? blockGapInProperties : 1;
        // offline mode: 10 seconds
//        return Constants.isOffline ? 10 : (gap*60); 
        return gap*60; 
    }

    /**
     * Read network definition from environment firstly.
     * Then read the definition from properties file.
     *
     * @return network definition
     */
    private static final String loadNetworkDefinition() {
        String networkInEnv = System.getProperty(RuntimeEnvironment.NETWORK_ARG);
        if (StringUtils.isNotBlank(networkInEnv)) return networkInEnv;

        return networkInProperties;
    }

    private static final String parseBootNodeHost() {
        if(isMainnet()){
            return "boot.sharder.io";
        }else if(isTestnet()){
            return "testboot.sharder.io";
        }
        return "devboot.sharder.io";
    }
    
    private static final List<String> parseBootNodesHost() {
       if(isMainnet()){
           return Lists.newArrayList("boot.sharder.io");
       }else if(isTestnet()){
           return Lists.newArrayList("testboot.sharder.io","testna.sharder.io","testnb.sharder.io");
       }
       return Lists.newArrayList("devboot.sharder.io");
    }
    
    
    public static synchronized boolean isValidBootNode(Peer peer){
        return bootNodesHost.contains(peer.getHost()) || bootNodesHost.contains(peer.getAnnouncedAddress());
    }
    
    public static synchronized boolean isValidBootNode(String peerHost){
        if(StringUtils.isEmpty(peerHost)) return false;
        return bootNodesHost.contains(peerHost);
    }

    public static String getBootNodeRandom(){
        return bootNodesHost.get(new Random().nextInt(bootNodesHost.size()));
    }

    public static final String SUCCESS = "success";

    public static final String DATA = "data";

    public static final String STATUS = "status";

    public static final String HTTP = "http://";

    public static final String CURLY_BRACES = "{";

    public static final String BRACKET = "[";

    public static final String HOST_FILTER_INFO = "Not valid host! ONLY {} can do this operation!";
    
    /** log count check key **/
    public static final String Generator_getNextGenerators = Generator.class.getName() + "#getNextGenerators";
    public static final String Generator_isMintHeightReached = Generator.class.getName() + "#isMintHeightReached";
    public static final String Generator_checkOrStartAutoMining = Generator.class.getName() + "#checkOrStartAutoMining";
    public static final String Generator_isBlockStuckOnBootNode = Generator.class.getName() + "#isBlockStuckOnBootNode";
    public static final String Generator_isPocTxsProcessed = Generator.class.getName() + "#isPocTxsProcessed";
    public static final String CONCH_P_reachLastKnownBlock = Conch.class.getName() + "#reachLastKnownBlock";
    public static final String BlockchainProcessor_P_downloadPeer = BlockchainProcessorImpl.class.getName() + "#downloadPeer";
    public static final String BlockchainProcessor_P_getMoreBlocks = BlockchainProcessorImpl.class.getName() + "#getMoreBlocks";
    public static final String GetNodeHardware_P_report = GetNodeHardware.class.getName() + "#report";
    
    public static final boolean hubLinked = Conch.getBooleanProperty("sharder.HubBind");
    public static final boolean initFromArchivedDbFile = Conch.getBooleanProperty("sharder.initFromArchivedDbFile");

}
