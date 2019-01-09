package org.conch.consensus.reward;

import org.conch.common.Constants;
import org.conch.consensus.poc.PocProcessorImpl;

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
    public static long mintReward(Long accountId) {
        // 300SS reward of one block 
        // 90% for hub miner, 10% for other miner in Testnet phase1 (before end of 2019.Q2)
        double rate = PocProcessorImpl.isHubBind(accountId) ? 0.9d : 0.1d;
        return Constants.ONE_SS * RewardDef.MINT.getAmount() * Math.round(rate);
    }
}