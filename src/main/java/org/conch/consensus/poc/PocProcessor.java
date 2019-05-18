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

import org.conch.account.Account;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.Transaction;

import java.util.Set;

/**
 * @author ben-xy
 */
public interface PocProcessor {
    
    /**
     * @param account
     * @param height
     * @return poc score 
     */
    PocScore calPocScore(Account account, int height);

    /**
     * Get the poc weight table
     *
     * @param version template version
     * @return PocWeightTable
     */
    PocTxBody.PocWeightTable getPocWeightTable(Long version);


    /**
     * account whether bound to certified peer
     *
     * @param accountId
     * @return
     */
    boolean isCertifiedPeerBind(long accountId);

    /**
     * get account linked certified peer
     *
     * @param accountId
     * @return
     */
    CertifiedPeer getLinkedPeer(long accountId);
    
    /**
     * get bind peer type
     * @param accountId
     * @return
     */
    Peer.Type bindPeerType(long accountId);

    /**
     * update bound account of certified peer
     * @param host peer host 
     * @param accountId bind acccount id
     */
    void updateBoundPeer(String host, long accountId);

    /**
     * clear current certified peers and re-syn 
     * @return 
     */
    boolean resetCertifiedPeers();

    /**
     * PoC tx process
     * @param tx poc tx
     * @return
     */
    boolean pocTxProcess(Transaction tx);

    /**
     * process delayed poc txs
     * @return
     */
    boolean processDelayedPocTxs(int height);
            
    /**
     * 
     * @return
     */
    boolean pocTxsProcessed(int height);

    
    boolean removeDelayedPocTxs(Set<Long> txIds);

    /**
     * notify poc processor to re-process the all poc txs
     */
    void notifySynTxNow();
    
//    void saveToDisk();
}
