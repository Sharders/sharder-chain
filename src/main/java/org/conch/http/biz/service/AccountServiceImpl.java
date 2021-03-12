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

package org.conch.http.biz.service;

import org.conch.crypto.Crypto;
import org.conch.http.biz.BizCoreProcess;
import org.conch.http.biz.domain.Account;
import org.conch.tools.PassPhraseGenerator;
import org.conch.tx.Attachment;
import org.conch.util.Convert;

import java.util.HashMap;

public class AccountServiceImpl implements AccountService {

    @Override
    public Account create(String passPhrase) throws Exception {
        String newAccountPassPhrase = PassPhraseGenerator.makeMnemonicWords();
        byte[] senderPublicKey;
        byte[] publicKey;
        String secretPhrase = Convert.emptyToNull(passPhrase);
        senderPublicKey = Crypto.getPublicKey(secretPhrase);
        publicKey = Crypto.getPublicKey(newAccountPassPhrase);

        long recipientId = org.conch.account.Account.getId(publicKey);
        org.conch.account.Account senderAccount = org.conch.account.Account.getAccount(senderPublicKey);
        // Send a message to active account
        HashMap<String, String> params = new HashMap<>();
        params.put("recipientPublicKey", Crypto.getPublicKey(newAccountPassPhrase).toString());
        params.put("recipient", org.conch.account.Account.rsAccount(recipientId));
        params.put("message",  org.conch.account.Account.rsAccount(recipientId) + " was created and broadcast to the network");
        params.put("deadline","60");
        params.put("feeNQT","0");
        BizCoreProcess.createTransaction(params, senderAccount, recipientId, 0, Attachment.ARBITRARY_MESSAGE);

        Account account = new Account();
        account.setAccountID(Long.toUnsignedString(recipientId));
        account.setAccountRS(org.conch.account.Account.rsAccount(recipientId));
        account.setPassPhrase(newAccountPassPhrase);
        return account;
    }
}
