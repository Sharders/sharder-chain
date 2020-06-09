package org.conch.consensus.burn;

import org.conch.Conch;
import org.conch.common.Constants;

/**
 * Burn calculator
 */
public class BurnCalculator {

    private static Double burnRate(){

        return 0.3;
    }

    /**
     *
     * @param txFees unit is NQT
     * @return
     */
    public static long burnAmount(long txFees) {
        if(Constants.BURN_OPENING_HEIGHT == -1L) return -1;
        if(txFees <= 0) return -1;
        if(Conch.getHeight() < Constants.BURN_OPENING_HEIGHT) return -1;

        return Math.round(txFees * burnRate());
    }
}