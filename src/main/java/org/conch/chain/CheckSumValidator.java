package org.conch.chain;

import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.tx.TransactionImpl;
import org.conch.util.Listener;
import org.conch.util.Logger;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/11
 */
public class CheckSumValidator {

    public static final int CHECKSUM_BLOCK_16 = 0;
    public static final int CHECKSUM_BLOCK_17 = 0;
    public static final int CHECKSUM_BLOCK_18 = 0;
    public static final int CHECKSUM_BLOCK_19 = 0;
    public static final int CHECKSUM_BLOCK_20 = 0;
    public static final int CHECKSUM_BLOCK_21 = 0;
    public static final int CHECKSUM_BLOCK_22 = 0;


    private static final byte[] CHECKSUM_TRANSPARENT_FORGING =
            new byte[] {
                    -122, -111, -35, 76, 59, 79, -75, 117, 34, 2, -70, -65, -38, 59, 0, 57,
                    120, 0, -107, 11, 97, -48, 21, 36, 48, -94, 88, 54, -14, 60, -101, -80
            };
    private static final byte[] CHECKSUM_NQT_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    110, -1, -56, -56, -58, 48, 43, 12, -41, -37, 90, -93, 80, 20, 3, -76, -84, -15, -113,
                    -34, 30, 32, 57, 85, -30, 16, -10, 127, -101, 17, 121, 124
            }
                    : new byte[] {
                    -90, -42, -57, -76, 88, -49, 127, 6, -47, -72, -39, -56, 51, 90, -90, -105,
                    121, 71, -94, -97, 49, -24, -12, 86, 7, -48, 90, -91, -24, -105, -17, -104
            };
    private static final byte[] CHECKSUM_MONETARY_SYSTEM_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    119, 51, 105, -101, -74, -49, -49, 19, 11, 103, -84, 80, -46, -5, 51, 42,
                    84, 88, 87, -115, -19, 104, 49, -93, -41, 84, -34, -92, 103, -48, 29, 44
            }
                    : new byte[] {
                    -117, -101, 74, 111, -114, 39, 80, -67, 48, 86, 68, 106, -105, 2, 84, -109,
                    1, 4, -20, -82, -112, -112, 25, 119, 23, -113, 126, -121, -36, 15, -32, -24
            };
    private static final byte[] CHECKSUM_PHASING_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    4, -100, -26, 47, 93, 1, -114, 86, -42, 46, -103, 13, 120, 0, 2, 100, -52, -67, 109,
                    -90, 87, 13, 30, -110, -58, -70, -94, 21, 105, -58, 20, 0
            }
                    : new byte[] {
                    -88, -128, 68, -118, 10, -62, 110, 19, -73, 61, 34, -76, 35, 73, -101, 9,
                    33, -111, 40, 114, 27, 105, 54, 0, 16, -97, 115, -12, -110, -88, 1, -15
            };
    private static final byte[] CHECKSUM_16 =
            Constants.isTestnet()
                    ? new byte[] {
                    -12, 21, 56, 106, -58, -126, 123, 33, 117, 11, -79, 28, -79, -45, 7, 69,
                    120, 71, -3, 27, 67, -85, 30, -25, -12, 127, 76, -60, -114, 41, -46, 55
            }
                    : new byte[] {
                    4, -96, 70, -17, 32, 17, 76, -92, 127, -127, 76, -77, 38, 7, 36, -113, 69, 26, -91, -94,
                    -81, -70, 62, 30, 114, 63, -102, -55, -75, 25, -17, -12
            };
    private static final byte[] CHECKSUM_17 =
            Constants.isTestnet()
                    ? new byte[] {
                    -19, -44, -49, 101, 5, -57, 51, 119, 16, 36, -3, 123, 90, -83, 89, 55, 72, 116, 4, 27,
                    -14, 114, 28, 79, -104, 100, -74, 61, -64, -6, -53, 103
            }
                    : new byte[] {
                    90, 15, -6, -42, -105, -103, 83, -17, -112, 51, -53, 110, 98, -54, -4, 2,
                    30, -69, 25, 91, 52, 126, -40, -91, -23, 118, -121, 70, 116, 60, -49, -86
            };
    private static final byte[] CHECKSUM_18 =
            Constants.isTestnet()
                    ? new byte[] {
                    98, 53, 16, 32, 124, -49, 117, -11, 50, -122, 110, 5, -47, -11, -36, -48,
                    -12, 10, -68, -105, 125, -61, -61, -62, -98, -64, -20, -110, 96, 20, 116, -52
            }
                    : new byte[] {
                    28, -67, 28, 87, -21, 91, -74, 115, -37, 67, 74, -32, -92, 53, -58, 62,
                    -60, 54, 58, -94, 9, 5, 26, -103, -19, 47, 78, 117, -49, 42, -14, 109
            };
    private static final byte[] CHECKSUM_19 =
            Constants.isTestnet()
                    ? new byte[] {
                    83, -5, -8, 51, 67, 28, 11, 101, -77, -57, 98, -113, 76, 2, -5, -97, -102, 112, -51,
                    -128, 79, -66, -81, -76, 113, 14, 51, 117, -77, -98, 84, 104
            }
                    : new byte[] {
                    -19, -60, 82, 93, 111, 77, 127, 100, 77, 102, 29, 104, 39, 78, -123, -108,
                    -25, -42, 59, 22, -9, -110, -32, -126, -18, 31, -46, 102, -75, -113, 6, 104
            };
    private static final byte[] CHECKSUM_20 =
            Constants.isTestnet()
                    ? new byte[] {
                    33, -16, 85, -127, -102, 97, 22, -52, -13, 24, 102, -53, -106, 35, 20, 33,
                    78, 37, -43, 63, 21, -59, -20, 120, 5, 80, 93, 71, -98, -58, 78, 95
            }
                    : new byte[] {
                    -31, 16, 18, -38, -86, 3, -111, -9, 3, -32, 87, 8, 70, 35, -33, -56, 91, -72, -55, -96,
                    -120, -127, -116, 2, -21, -89, -7, 56, -114, -66, 72, -49
            };
    private static final byte[] CHECKSUM_21 =
            Constants.isTestnet()
                    ? new byte[] {
                    -63, -51, -56, -76, 13, 21, -47, 69, -108, -28, -124, 108, -17, 27, 30, -65,
                    -95, 110, -9, 35, 59, 112, -20, 122, -44, -86, -54, 51, 46, 80, -13, -26
            }
                    : new byte[] {
                    -26, -121, -115, -116, -62, -120, -99, -74, -52, -39, 9, 52, 20, 92, -42, 115,
                    19, -67, 7, 51, 4, -100, -41, 41, 57, -102, 19, -128, -109, -52, -68, -15
            };
    private static final byte[] CHECKSUM_22 =
            Constants.isTestnet()
                    ? new byte[] {
                    122, 81, 72, -91, -63, 124, -29, -79, -27, -85, 25, 24, 59, 12, 118, -63,
                    118, 63, -71, 104, 103, 95, -107, -29, -48, 71, -59, 13, 71, -72, -82, -76
            }
                    : new byte[] {
                    -77, 16, -52, 88, -21, -67, -119, 121, 121, 120, -70, 88, 44, -99, -9, -42, 48, -77, 28,
                    40, 106, -48, 13, 30, -22, -122, 35, 22, 29, 2, -93, 94
            };


    private static final CheckSumValidator inst = new CheckSumValidator();

    public static CheckSumValidator getInst(){
        return inst;
    }

    /**
     * validate checksum after Event.BLOCK_SCANNED or Event.BLOCK_PUSHED
     * @return
     */
    public static Listener<Block> eventProcessor(){
        return inst.checksumListener;
    }
    
    // pop off to previous right height when checksum validation failed
    private final Listener<Block> checksumListener =
            block -> {
                if (block.getHeight() == Constants.TRANSPARENT_FORGING_BLOCK) {
                    //TODO[checksum]
                    if (!verifyChecksum(CHECKSUM_TRANSPARENT_FORGING, 0, Constants.TRANSPARENT_FORGING_BLOCK)) {
                        Conch.getBlockchainProcessor().popOffTo(0);
                    }
                } else if (block.getHeight() == Constants.NQT_BLOCK) {
                    //TODO[checksum-NQT]
                    if (!verifyChecksum(CHECKSUM_NQT_BLOCK, Constants.TRANSPARENT_FORGING_BLOCK, Constants.NQT_BLOCK)) {
                        Conch.getBlockchainProcessor().popOffTo(Constants.TRANSPARENT_FORGING_BLOCK);
                    }
                } else if (block.getHeight() == Constants.MONETARY_SYSTEM_BLOCK) {
                    //FIXME[checksum-NQT]
                    if (!verifyChecksum(CHECKSUM_MONETARY_SYSTEM_BLOCK,Constants.NQT_BLOCK,Constants.MONETARY_SYSTEM_BLOCK)) {
                        Conch.getBlockchainProcessor().popOffTo(Constants.NQT_BLOCK);
                    }
                } else if (block.getHeight() == Constants.PHASING_BLOCK) {
                    if (!verifyChecksum(CHECKSUM_PHASING_BLOCK, Constants.MONETARY_SYSTEM_BLOCK, Constants.PHASING_BLOCK)) {
                        Conch.getBlockchainProcessor().popOffTo(Constants.MONETARY_SYSTEM_BLOCK);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_16) {
                    if (!verifyChecksum(CHECKSUM_16, Constants.PHASING_BLOCK, CHECKSUM_BLOCK_16)) {
                        Conch.getBlockchainProcessor().popOffTo(Constants.PHASING_BLOCK);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_17) {
                    if (!verifyChecksum(CHECKSUM_17, CHECKSUM_BLOCK_16, CHECKSUM_BLOCK_17)) {
                        Conch.getBlockchainProcessor().popOffTo(CHECKSUM_BLOCK_16);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_18) {
                    if (!verifyChecksum(CHECKSUM_18, CHECKSUM_BLOCK_17, CHECKSUM_BLOCK_18)) {
                        Conch.getBlockchainProcessor().popOffTo(CHECKSUM_BLOCK_17);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_19) {
                    if (!verifyChecksum(CHECKSUM_19, CHECKSUM_BLOCK_18, CHECKSUM_BLOCK_19)) {
                        Conch.getBlockchainProcessor().popOffTo(CHECKSUM_BLOCK_18);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_20) {
                    if (!verifyChecksum(CHECKSUM_20, CHECKSUM_BLOCK_19, CHECKSUM_BLOCK_20)) {
                        Conch.getBlockchainProcessor().popOffTo(CHECKSUM_BLOCK_19);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_21) {
                    if (!verifyChecksum(CHECKSUM_21, CHECKSUM_BLOCK_20, CHECKSUM_BLOCK_21)) {
                        Conch.getBlockchainProcessor().popOffTo(CHECKSUM_BLOCK_20);
                    }
                } else if (block.getHeight() == CHECKSUM_BLOCK_22) {
                    if (!verifyChecksum(CHECKSUM_22, CHECKSUM_BLOCK_21, CHECKSUM_BLOCK_22)) {
                        Conch.getBlockchainProcessor().popOffTo(CHECKSUM_BLOCK_21);
                    }
                }
            };


    private boolean verifyChecksum(byte[] validChecksum, int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE height > ? AND height <= ? ORDER BY id ASC, timestamp ASC")) {
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            try (DbIterator<TransactionImpl> iterator = BlockchainImpl.getInstance().getTransactions(con, pstmt)) {
                while (iterator.hasNext()) {
                    digest.update(iterator.next().getBytes());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        byte[] checksum = digest.digest();
        if (validChecksum == null) {
            Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
            return true;
        } else if (!Arrays.equals(checksum, validChecksum)) {
            Logger.logErrorMessage("Checksum failed at block " + Conch.getBlockchain().getHeight() + ": " + Arrays.toString(checksum));
            return false;
        } else {
            Logger.logMessage("Checksum passed at block " + Conch.getBlockchain().getHeight());
            return true;
        }
    }
}
