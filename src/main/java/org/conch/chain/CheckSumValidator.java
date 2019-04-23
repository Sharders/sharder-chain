package org.conch.chain;

import com.alibaba.fastjson.JSON;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.tx.TransactionImpl;
import org.conch.util.Listener;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/11
 */
public class CheckSumValidator {

    private static final byte[] CHECKSUM_POC_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    110, -1, -56, -56, -58, 48, 43, 12, -41, -37, 90, -93, 80, 20, 3, -76, -84, -15, -113,
                    -34, 30, 32, 57, 85, -30, 16, -10, 127, -101, 17, 121, 124
            }
                    : new byte[] {
                    -90, -42, -57, -76, 88, -49, 127, 6, -47, -72, -39, -56, 51, 90, -90, -105,
                    121, 71, -94, -97, 49, -24, -12, 86, 7, -48, 90, -91, -24, -105, -17, -104
            };
    
    // not opened yet
    private static final byte[] CHECKSUM_PHASING_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    -1
            }
                    : new byte[] {
                   -1
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
    private final Listener<Block> checksumListener = block -> {
                if (block.getHeight() == Constants.POC_BLOCK_HEIGHT) {
                    if (!verifyChecksum(CHECKSUM_POC_BLOCK, 0, Constants.POC_BLOCK_HEIGHT)) {
                        Conch.getBlockchainProcessor().popOffTo(0);
                    }
                } else if (block.getHeight() == Constants.PHASING_BLOCK_HEIGHT) {
                    if (!verifyChecksum(CHECKSUM_PHASING_BLOCK, Constants.POC_BLOCK_HEIGHT, Constants.PHASING_BLOCK_HEIGHT)) {
                        Conch.getBlockchainProcessor().popOffTo(Constants.POC_BLOCK_HEIGHT);
                    }
                }
            };


    private boolean verifyChecksum(byte[] validChecksum, int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE height > ? AND height <= ? ORDER BY id ASC, timestamp ASC");
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            try (DbIterator<TransactionImpl> iterator = BlockchainImpl.getInstance().getTransactions(con, pstmt)) {
                while (iterator.hasNext()) {
                    digest.update(iterator.next().getBytes());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
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
    
    
    /**  **/
    // known ignore blocks
    private static final long[] knownIgnoreBlocks = new long[] {
            //Testnet
            -8556361949057624360L,
            211456030592803100L
    };
    static {
        Arrays.sort(knownIgnoreBlocks);
    }


    public static boolean isKnownIgnoreBlock(long blockId){
        return Arrays.binarySearch(knownIgnoreBlocks, blockId) >= 0;
    }

    public static void updateKnownIgnoreBlocks(){
        RestfulHttpClient.HttpResponse response = null;
        try {
            response = RestfulHttpClient.getClient(UrlManager.getKnownIgnoreBlockUrl()).get().request();
            com.alibaba.fastjson.JSONArray result = JSON.parseArray(response.getContent());
            

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
