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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public final class Constants {

    public static final boolean isTestnet = Conch.getBooleanProperty("sharder.isTestnet");
    public static final boolean isOffline = Conch.getBooleanProperty("sharder.isOffline");
    public static final boolean isLightClient = Conch.getBooleanProperty("sharder.isLightClient");

    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MIN_TRANSACTION_SIZE = 176;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * MIN_TRANSACTION_SIZE * 244;
    public static final long MAX_BALANCE_SS = 1000000000;
    public static final long ONE_SS = 100000000;
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_SS * ONE_SS;

    public static final long INITIAL_BASE_TARGET = 153722867 * 8;
    public static final int MIN_BLOCKTIME_LIMIT = 53;
    public static final int MAX_BLOCKTIME_LIMIT = 67;
    public static final int BASE_TARGET_GAMMA = 64;
    public static final int BLOCK_GAP = isTestnet ?  Conch.getIntProperty("sharder.testnetBlockGap") : Conch.getIntProperty("sharder.blockGap");

//    public static final long INITIAL_BASE_TARGET = 6000;
//    public static final int MIN_BLOCKTIME_LIMIT = 17;
//    public static final int MAX_BLOCKTIME_LIMIT = 22;
//    public static final int BASE_TARGET_GAMMA = 21;

    public static final int MAX_ROLLBACK = Math.max(Conch.getIntProperty("sharder.maxRollback"), 720);
    public static final long MAX_BASE_TARGET = MAX_BALANCE_SS * INITIAL_BASE_TARGET;
    public static final long MAX_BASE_TARGET_2 = isTestnet ? MAX_BASE_TARGET : INITIAL_BASE_TARGET * 50;
    public static final long MIN_BASE_TARGET = INITIAL_BASE_TARGET * 9 / 10;


//  public static final int GUARANTEED_BALANCE_CONFIRMATIONS = isTestnet ? Conch.getIntProperty("sharder.testnetGuaranteedBalanceConfirmations", 1440) : 1440;
//  public static final int LEASING_DELAY = isTestnet ? Conch.getIntProperty("sharder.testnetLeasingDelay", 1440) : 1440;
    //内测期间只需要10次确认
    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = isTestnet ? Conch.getIntProperty("sharder.testnetGuaranteedBalanceConfirmations", 10) : 10;
    public static final int LEASING_DELAY = isTestnet ? Conch.getIntProperty("sharder.testnetLeasingDelay", 10) : 10;
    public static final long MIN_FORGING_BALANCE_NQT = 1000 * ONE_SS;

    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference
//    public static final int FORGING_DELAY = 600; // 内测阶段设为10分钟延迟
    public static final int FORGING_DELAY = Conch.getIntProperty("sharder.forgingDelay");
    public static final int FORGING_SPEEDUP = Conch.getIntProperty("sharder.forgingSpeedup");

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

    public static final int MIN_PRUNABLE_LIFETIME = isTestnet ? 1440 * 60 : 14 * 1440 * 60;
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
    public static final short SHUFFLING_PROCESSING_DEADLINE = (short)(isTestnet ? 10 : 100);

    public static final int MAX_TAGGED_DATA_NAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGGED_DATA_TAGS_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_CHANNEL_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_FILENAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DATA_LENGTH = 10 * 1024 * 1024;
    public static final int MAX_RESPONSE_SIZE = 20 * 1024 * 1024;

    /**conch**/
    public static final int ALIAS_SYSTEM_BLOCK = 0;
    public static final int TRANSPARENT_FORGING_BLOCK = 0;
    public static final int ARBITRARY_MESSAGES_BLOCK = 0;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = 0;
    public static final int TRANSPARENT_FORGING_BLOCK_DIRECT = 3000; //此节点数之前官方节点可直接锻造
    public static final int TRANSPARENT_FORGING_BLOCK_4 = 0;
    public static final int TRANSPARENT_FORGING_BLOCK_5 = 0;
    public static final int TRANSPARENT_FORGING_BLOCK_6 = isTestnet ? 0 : 0;
    public static final int TRANSPARENT_FORGING_BLOCK_7 = Integer.MAX_VALUE;
    public static final int TRANSPARENT_FORGING_BLOCK_8 = isTestnet ? 0 : 0;
    public static final int NQT_BLOCK = isTestnet ? 0 : 0;
    public static final int CONCH_BV_BLOCK = isTestnet ? 43000 : 43000;
    public static final int FRACTIONAL_BLOCK = isTestnet ? NQT_BLOCK : 0;
    public static final int ASSET_EXCHANGE_BLOCK = isTestnet ? NQT_BLOCK : 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK = isTestnet ? NQT_BLOCK : 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP = isTestnet ? 0 : 0;
    public static final int DIGITAL_GOODS_STORE_BLOCK = isTestnet ? 0 : 0;
    public static final int MONETARY_SYSTEM_BLOCK = isTestnet ? 0 : 0;
    public static final int PHASING_BLOCK = isTestnet ? 0 : 0;
    public static final int CHECKSUM_BLOCK_16 = isTestnet ? 0 : 0;
    public static final int SHUFFLING_BLOCK = isTestnet ? 0 : 0;
    public static final int CHECKSUM_BLOCK_17 = isTestnet ? 0 : 0;
    public static final int CHECKSUM_BLOCK_18 = isTestnet ? 0 : 0;
    public static final int CHECKSUM_BLOCK_19 = isTestnet ? 0 : 0;
    public static final int FXT_BLOCK = isTestnet ? 10000 : 10000; //封闭内测块高度
    public static final int CHECKSUM_BLOCK_20 = isTestnet ? 0 : 0;
    public static final int CHECKSUM_BLOCK_21 = isTestnet ? 0 : 0;
    public static final int CHECKSUM_BLOCK_22 = isTestnet ? 0 : 0;

    public static final int MAX_REFERENCED_TRANSACTION_TIMESPAN = 60 * 1440 * 60;

    public static final int LAST_CHECKSUM_BLOCK = CHECKSUM_BLOCK_22;
    // LAST_KNOWN_BLOCK must also be set in html/www/js/nrs.constants.js
    public static final int LAST_KNOWN_BLOCK = CHECKSUM_BLOCK_22;

    public static final int[] MIN_VERSION = new int[] {0, 0, 1};
    public static final int[] MIN_PROXY_VERSION = new int[] {0, 0, 1};

    static final long UNCONFIRMED_POOL_DEPOSIT_NQT = (isTestnet ? 50 : 100) * ONE_SS;
    public static final long SHUFFLING_DEPOSIT_NQT = (isTestnet ? 7 : 1000) * ONE_SS;

    public static final boolean correctInvalidFees = Conch.getBooleanProperty("sharder.correctInvalidFees");
    public static final String ACCOUNT_PREFIX = "SSA-"; //account prefix, you can replace all in files to redefine it

    public static final long EPOCH_BEGINNING;

    public static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    //Conch chain begin time
    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2016);
        calendar.set(Calendar.MONTH, Calendar.AUGUST);
        calendar.set(Calendar.DAY_OF_MONTH, 16);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH_BEGINNING = calendar.getTimeInMillis();
    }

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Constants() {} // never

    static final ArrayList<Long> configFee = new ArrayList<>();

    static {
        configFee.add((long)Conch.getIntProperty("sharder.fee.payment"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.message"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.coloredCions"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.digitalGoods"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.accountControl"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.monetarySystem"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.dataFee"));
        configFee.add((long)Conch.getIntProperty("sharder.fee.shuffling"));
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
}
