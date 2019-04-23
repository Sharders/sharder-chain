package org.conch.consensus.reward;

import org.conch.Conch;
import org.conch.common.Constants;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/8
 */
public class RewardCalculator {

    /**
     * Reward definition, amount is the reward amount
     */
    public enum RewardDef {
        MINT(128);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }
        
    }
    
    private static final int HALVE_COUNT = 210240;
    /**
     * mint reward calculate
     * @return
     */
    public static long mintReward() {
        double turn = 0d;
        if(Conch.getBlockchain().getHeight() > HALVE_COUNT){
            turn = Conch.getBlockchain().getHeight() / HALVE_COUNT;
        }
        double rate = Math.pow(0.5d,turn);
        return (long)(Constants.ONE_SS * RewardDef.MINT.getAmount() * rate);
    }
}