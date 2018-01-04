package org.conch.cpos.core;

import org.conch.Attachment;
import org.conch.Constants;
import org.conch.ConchException;
import org.conch.TransactionImpl;
import org.conch.util.Logger;

import java.util.Calendar;
import java.util.List;

/**
 * @author  xy@ichaoj.com
 * @version 16/8/3
 */
public class RewardIssuer {

    private RewardIssuer() {}

    private static final int REWARD_COUNT = 10;
    public static final long FORGE_ACCOUNT_ID = 9011521658538046719L;
    private static final byte[] FORGE_FUND_PK = {45, -47, 43, 69, 124, 115, -15, -34, -45, -65, 5, 101, 3, 76, 24, 67, -20, -128, 72, -93, -39, -106, 78, -22, 41, -34, 85, -118, -16, 50, 8, 89};
    private static final String FORGE_FUND_SP = "finish rant princess crimson cold forward such known lace built poetry ceiling";
    public static TransactionImpl forgeReward(int height,long rewardUserId,int timestamp) {
        try {
            //transcation version=1, deadline=10,timestamp=currentTime-timeDiff
            return new TransactionImpl.BuilderImpl((byte) 1, FORGE_FUND_PK,
                    REWARD_COUNT * Constants.ONE_NXT, 0, (short) 10,
                    Attachment.ORDINARY_PAYMENT)
                    .timestamp(timestamp)
                    .recipientId(rewardUserId)
                    .height(height)
                    .build(FORGE_FUND_SP);
        } catch (ConchException.NotValidException e) {
            Logger.logErrorMessage("Can't generate reward transcation[rewardUserId=" + rewardUserId + "]",e);
        }
        return null;
    }

    private static Calendar calendar = null;
    private static int getRewardCount(){
        if(calendar == null) {
            calendar = Calendar.getInstance();
            calendar.set(2017,7,14,20,0,0);
        }
        return System.currentTimeMillis() >= calendar.getTimeInMillis() ? REWARD_COUNT : 1;
    }

    public static List<TransactionImpl> addRewardIntoTransactions(List<TransactionImpl> transactions,int height,long rewardUserId,int timestamp){
        transactions.add(forgeReward(height,rewardUserId,timestamp));
        return transactions;
    }

    public static void main(String[] args) {
        System.out.println(getRewardCount());
    }
}

