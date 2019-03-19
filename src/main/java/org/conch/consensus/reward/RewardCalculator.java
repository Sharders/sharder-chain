package org.conch.consensus.reward;

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
        MINT(300);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }
        
    }

    /**
     * mint reward calculate
     * @param accountId
     * @return
     */
    public static long mintReward(long accountId) {
        // 300SS reward of one block
        return Constants.ONE_SS * RewardDef.MINT.getAmount();
    }
}