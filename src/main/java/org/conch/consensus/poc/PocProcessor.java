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

package org.conch.consensus.poc;

import com.alibaba.fastjson.JSONObject;
import org.conch.account.Account;
import org.conch.consensus.poc.tx.PocTxBody;

import java.math.BigInteger;

/**
 * @author ben-xy
 */
public interface PocProcessor {
    
    public static final String SCORE_KEY = "poc_score";
    /**
     * @param account
     * @param height
     * @return poc score json
     */
    JSONObject calPocScore(Account account, int height);

    /**
     * return detailed poc data json string
     *
     * @param scoreJson
     * @return poc score
     */
    BigInteger getScoreInt(JSONObject scoreJson);

    /**
     * Get the poc weight table
     *
     * @param version template version
     * @return PocWeightTable
     */
    PocTxBody.PocWeightTable getPocWeightTable(Long version);
}
