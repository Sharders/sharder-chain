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

package org.conch.mint;

import org.conch.asset.token.Currency;
import org.conch.crypto.HashFunction;
import org.conch.tx.Attachment;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class CurrencyMinting {

    public static final Set<HashFunction> acceptedHashFunctions =
            Collections.unmodifiableSet(EnumSet.of(HashFunction.SHA256, HashFunction.SHA3, HashFunction.SCRYPT, HashFunction.Keccak25));

    public static boolean meetsTarget(long accountId, Currency currency, Attachment.MonetarySystemCurrencyMinting attachment) {
        byte[] hash = getHash(currency.getAlgorithm(), attachment.getNonce(), attachment.getCurrencyId(), attachment.getUnits(),
                attachment.getCounter(), accountId);
        byte[] target = getTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(),
                attachment.getUnits(), currency.getCurrentSupply() - currency.getReserveSupply(), currency.getMaxSupply() - currency.getReserveSupply());
        return meetsTarget(hash, target);
    }

    public static boolean meetsTarget(byte[] hash, byte[] target) {
        for (int i = hash.length - 1; i >= 0; i--) {
            if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                return false;
            }
            if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                return true;
            }
        }
        return true;
    }

    public static byte[] getHash(byte algorithm, long nonce, long currencyId, long units, long counter, long accountId) {
        HashFunction hashFunction = HashFunction.getHashFunction(algorithm);
        return getHash(hashFunction, nonce, currencyId, units, counter, accountId);
    }

    public static byte[] getHash(HashFunction hashFunction, long nonce, long currencyId, long units, long counter, long accountId) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
        buffer.putLong(accountId);
        return hashFunction.hash(buffer.array());
    }

    public static byte[] getTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply) {
        return getTarget(getNumericTarget(min, max, units, currentMintableSupply, totalMintableSupply));
    }

    public static byte[] getTarget(BigInteger numericTarget) {
        byte[] targetRowBytes = numericTarget.toByteArray();
        if (targetRowBytes.length == 32) {
            return reverse(targetRowBytes);
        }
        byte[] targetBytes = new byte[32];
        Arrays.fill(targetBytes, 0, 32 - targetRowBytes.length, (byte) 0);
        System.arraycopy(targetRowBytes, 0, targetBytes, 32 - targetRowBytes.length, targetRowBytes.length);
        return reverse(targetBytes);
    }

    public static BigInteger getNumericTarget(Currency currency, long units) {
        return getNumericTarget(currency.getMinDifficulty(), currency.getMaxDifficulty(), units,
                currency.getCurrentSupply() - currency.getReserveSupply(), currency.getMaxSupply() - currency.getReserveSupply());
    }

    public static BigInteger getNumericTarget(int min, int max, long units, long currentMintableSupply, long totalMintableSupply) {
        if (min < 1 || max > 255) {
            throw new IllegalArgumentException(String.format("Min: %d, Max: %d, allowed range is 1 to 255", min, max));
        }
        int exp = (int)(256 - min - ((max - min) * currentMintableSupply) / totalMintableSupply);
        return BigInteger.valueOf(2).pow(exp).subtract(BigInteger.ONE).divide(BigInteger.valueOf(units));
    }

    private static byte[] reverse(byte[] b) {
        for(int i=0; i < b.length/2; i++) {
            byte temp = b[i];
            b[i] = b[b.length - i - 1];
            b[b.length - i - 1] = temp;
        }
        return b;
    }

    private CurrencyMinting() {} // never

}
