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

package org.conch.asset;

import org.conch.account.Account;
import org.conch.account.AccountLedger;

public enum HoldingType {

    SS((byte)0) {

        @Override
        public long getBalance(Account account, long holdingId) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            return account.getBalanceNQT();
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            return account.getUnconfirmedBalanceNQT();
        }

        @Override
        public void addToBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            account.addToBalanceNQT(event, eventId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            account.addToUnconfirmedBalanceNQT(event, eventId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            if (holdingId != 0) {
                throw new IllegalArgumentException("holdingId must be 0");
            }
            account.addToBalanceAndUnconfirmedBalanceNQT(event, eventId, amount);
        }

    },

    ASSET((byte)1) {

        @Override
        public long getBalance(Account account, long holdingId) {
            return account.getAssetBalanceQNT(holdingId);
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            return account.getUnconfirmedAssetBalanceQNT(holdingId);
        }

        @Override
        public void addToBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToAssetBalanceQNT(event, eventId, holdingId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToUnconfirmedAssetBalanceQNT(event, eventId, holdingId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToAssetAndUnconfirmedAssetBalanceQNT(event, eventId, holdingId, amount);
        }

    },

    CURRENCY((byte)2) {

        @Override
        public long getBalance(Account account, long holdingId) {
            return account.getCurrencyUnits(holdingId);
        }

        @Override
        public long getUnconfirmedBalance(Account account, long holdingId) {
            return account.getUnconfirmedCurrencyUnits(holdingId);
        }

        @Override
        public void addToBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToCurrencyUnits(event, eventId, holdingId, amount);
        }

        @Override
        public void addToUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToUnconfirmedCurrencyUnits(event, eventId, holdingId, amount);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount) {
            account.addToCurrencyAndUnconfirmedCurrencyUnits(event, eventId, holdingId, amount);
        }

    };

    public static HoldingType get(byte code) {
        for (HoldingType holdingType : values()) {
            if (holdingType.getCode() == code) {
                return holdingType;
            }
        }
        throw new IllegalArgumentException("Invalid holdingType code: " + code);
    }

    private final byte code;

    HoldingType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public abstract long getBalance(Account account, long holdingId);

    public abstract long getUnconfirmedBalance(Account account, long holdingId);

    public abstract void addToBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount);

    public abstract void addToUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount);

    public abstract void addToBalanceAndUnconfirmedBalance(Account account, AccountLedger.LedgerEvent event, long eventId, long holdingId, long amount);

}
