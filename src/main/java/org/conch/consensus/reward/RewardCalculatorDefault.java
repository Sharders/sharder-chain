package org.conch.consensus.reward;

import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.util.LocalDebugTool;


/**
 * @author bowen
 * @since 2021/03/12
 */
public class RewardCalculatorDefault extends RewardCalculator {

    /**
     * Reward definition
     * NOTE: Mining Reward = Block Reward - Crowd Miner Reward
     */
    private enum RewardDef {
        BLOCK_REWARD(180*Constants.ONE_SS),
        CROWD_MINERS_REWARD(1 * Constants.ONE_SS),
        STABLE_PHASE_BLOCK_REWARD(1 * Constants.ONE_SS),
        STABLE_PHASE_CROWD_MINERS_REWARD(1 * Constants.ONE_SS / 2);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }

    }

    /**
     * how much one block reward
     * @return
     */
    @Override
    public long blockReward(int height) {
        // Halving logic
        if(HALVE_COUNT != -1) {
            double turn = 0d;
            if(Conch.getBlockchain().getHeight() > HALVE_COUNT){
                turn = Conch.getBlockchain().getHeight() / HALVE_COUNT;
            }
            double rate = Math.pow(0.5d, turn);
            return (long)(RewardDef.BLOCK_REWARD.getAmount() * rate);
        }

        // No block rewards in the miner joining phase
        if(height <= NETWORK_STABLE_PHASE) {
            return RewardDef.STABLE_PHASE_BLOCK_REWARD.getAmount();
        } else {
            return RewardDef.BLOCK_REWARD.getAmount();
        }
    }

    @Override
    public long crowdMinerReward(int height){
        if(height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT
        || LocalDebugTool.isLocalDebugAndBootNodeMode){
            if(height <= NETWORK_STABLE_PHASE) {
                return RewardDef.STABLE_PHASE_CROWD_MINERS_REWARD.getAmount();
            } else {
                return RewardDef.CROWD_MINERS_REWARD.getAmount();
            }
        }
        return 0L;
    }

    /**
     * Whether reach crowd reward height
     *
     * @param height
     * @return
     */
    @Override
    public int getRewardSettlementHeight(int height) {
        return Constants.SETTLEMENT_INTERVAL_SIZE;
    }

}
