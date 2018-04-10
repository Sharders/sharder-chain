package nxt.cpos.core;

import nxt.Account;
import nxt.util.Logger;

/**
 * 海螺链的创世节点,按照如下方式分配初始资金=> 以下为测试数据.
 * @author  xy@ichaoj.com
 * @version 16/7/1
 */
public class ConchGenesis {

    public static final long GENESIS_BLOCK_ID = 7690000000000000000L;
    public static final long CREATOR_ID = 7690000000000000000L;
    public static final byte[] CREATOR_PUBLIC_KEY = {
            15, -4, -120, 50, -66, -107, 70, -54, -95, 61, -14,
            79, 123, -18, -57, -99, 10, -34, 75, -48, -72, -25, 96, -53, -63, -1, 43
    };
    public static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
           52, 111, 94, 0, 87, 23, 22, 86, -91, 89, -37, 84, 29,
            48, 18, 15, -125, 97, -103, 106, -104, -125, -104, -33, 110, 99, -1, -79, -116, 25, 6, 73, 64, 34, 108, -33, 56, 107, -73, -60,
            17, 91, 104, -115, 67, -94, 3, -92
    };

    public static final int[] GENESIS_AMOUNTS = {
            50000000,
            10000000,
            20000000,
            5000000,
            10000000,
            10000000
    };

    public static final long[] GENESIS_RECIPIENTS = {
            Long.parseUnsignedLong("7690000000000000000L"), //ICO Fund
            Long.parseUnsignedLong("7690000000000000000L"), //Community Fund
            Long.parseUnsignedLong("7690000000000000000L"), //BusinessApp Fund
            Long.parseUnsignedLong("7690000000000000000L"), //Operation Fund
            Long.parseUnsignedLong("7690000000000000000L"), //Forge Fund
            Long.parseUnsignedLong("7690000000000000000L")  //Reserve Fund
    };

    public static boolean isFoundAccount(long accountId){
        for(long foundId : GENESIS_RECIPIENTS){
            if(foundId == accountId) return true;
        }
        return false;
    }


    public static final byte[][] GENESIS_RECIPIENTS_PK = {
            {11, 5, 14, 84, -15, -59, 81, 105, 88, -43, 119, 56, 10, 34, 62, -93, 23, -122, 95, -14, 21, 35, 8, 58, -62, 6, -114, 52, -72, 35, 120, 17},
            {11, -123, -106, 33, 110, -45, 83, -18, 33, 92, 33, -38, -69, -88, 97, -2, 78, 1, -65, -112, -30, -40, 116, -2, 28, -1, -93, -5, -17, -17, -106, 125},
            {11, 104, -23, -64, -49, -101, -94, -46, -27, 31, -50, -79, -85, 98, 124, 107, 8, -29, -28, -92, 66, 17, 100, -29, 95, -11, -57, 25, -127, -54, 55, 9},
            {11, -53, 34, 78, 118, -8, -86, 30, -7, -81, -4, 50, -97, -102, 105, -58, 110, -119, 18, 99, -84, -66, -84, -36, -89, 35, 85, 58, -40, 118, 111, 56},
            {11, -47, 43, 69, 124, 115, -15, -34, -45, -65, 5, 101, 3, 76, 24, 67, -20, -128, 72, -93, -39, -106, 78, -22, 41, -34, 85, -118, -16, 50, 8, 89},
            {-11, -91, 37, 67, -50, -2, 94, 103, -33, 21, 113, 119, 110, 5, -114, -94, 102, -34, 19, 82, 36, 100, -7, 111, 84, 25, 18, 89, 42, -115, -102, 4}
    };


    public static final byte[][] GENESIS_RECIPIENTS_SIGNATURES = {
            {11, -14, 83, -53, 94, -13, -103, 52, -38, -125, -28, -123, 80, 90, -64, 121, 113, 27, 0, -84, -10, 117, 110, -20, -17, 14, 39, 67, 110, 79, 96, 121},
            {-11, 6, -1, 19, -50, -86, 29, 78, 117, 62, -40, 116, -42, -45, 24, 94, 13, -30, 98, 92, 108, -91, 25, -28, -47, -60, -108, 105, -107, -88, -117, -64},
            {11, 80, 60, 93, -12, 116, -50, 114, -24, 39, -96, 20, -120, -104, 62, 34, -82, -6, -119, -24, -38, 96, 101, 104, 125, -123, 56, 41, -118, -110, 52, -31},
            {11, 92, 65, 86, -19, 118, -114, 126, 74, -4, -4, 19, -14, 38, -77, -26, -32, -15, 68, 3, -51, -56, -70, 108, 31, -70, 19, -33, 9, -78, 63, 2},
            {-11, 52, -26, 14, 52, 91, 15, 125, 99, 38, 30, 60, -54, 90, 112, 75, -16, 16, 127, 58, -40, -21, -40, -7, -4, -49, -122, 21, 69, 41, 101, -53},
            {11, 60, -5, -107, -89, -106, 94, 44, -30, -114, 87, -53, -67, -36, 127, 9, -85, 118, -56, -67, -117, -4, -66, -35, -48, 106, 122, 109, -47, 66, -50, -48}
    };

    private static boolean enableOfficalAccount = false;
    public static final void enableOfficalAccount(){
        if(enableOfficalAccount) return;

        Logger.logDebugMessage("Enable offical accountp[size=" + GENESIS_RECIPIENTS.length + 1 + "]");

        Account.addOrGetAccount(CREATOR_ID).apply(CREATOR_PUBLIC_KEY);

        for(int i = 0 ; i < GENESIS_RECIPIENTS.length; i ++) {
            Account.addOrGetAccount(GENESIS_RECIPIENTS[i]).apply(GENESIS_RECIPIENTS_PK[i]);
        }

        enableOfficalAccount = true;
    }

    private ConchGenesis() {}
}

