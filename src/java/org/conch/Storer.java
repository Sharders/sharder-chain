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

package org.conch;

import org.conch.crypto.Crypto;
import org.conch.util.*;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Storer {

    public enum Event {
        START_STORING, STOP_STORING
    }

    private static Storer storer;

    static void init() {}


    public static Storer startStoring(String secretPhrase) {
        storer = new Storer(secretPhrase);
        Logger.logDebugMessage(storer + " started and previous one has been replaced");
        return storer;
    }

    public static void stopStoring(String secretPhrase) {
        if(storer != null) {
            String storerInfo = storer.toString();
            Conch.getBlockchain().updateLock();
            try {
                storer = null;
                StorageProcessorImpl.clearBackupTask();
            } finally {
                Conch.getBlockchain().updateUnlock();
            }
            Logger.logDebugMessage(storerInfo + " stopped");
        } else {
            Logger.logDebugMessage("storer already stopped");
        }
    }

    private final long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;

    private Storer(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getSecretPhrase() {
        return secretPhrase;
    }

    public static Storer getStorer() {
        return storer;
    }

    @Override
    public String toString() {
        return "Storer " + Long.toUnsignedString(accountId);
    }


}
