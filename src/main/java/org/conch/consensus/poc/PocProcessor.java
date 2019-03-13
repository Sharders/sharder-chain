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

    /**
     * @param account
     * @param height
     * @return
     */
    BigInteger calPocScore(Account account, int height);

    /**
     * return detailed poc data json string
     *
     * @param account
     * @param height
     * @return json string
     */
    JSONObject calDetailedPocScore(Account account, int height);

    /**
     * Get the poc weight table
     *
     * @param version template version
     * @return PocWeightTable
     */
    PocTxBody.PocWeightTable getPocWeightTable(Long version);
}
