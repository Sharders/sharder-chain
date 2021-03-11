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

package org.conch.crypto;

import org.conch.common.ConchException;
import org.conch.util.Convert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class AnonymouslyEncryptedData {

    public static AnonymouslyEncryptedData encrypt(byte[] plaintext, String secretPhrase, byte[] theirPublicKey, byte[] nonce) {
        byte[] keySeed = Crypto.getKeySeed(secretPhrase, theirPublicKey, nonce);
        byte[] myPrivateKey = Crypto.getPrivateKey(keySeed);
        byte[] myPublicKey = Crypto.getPublicKey(keySeed);
        byte[] sharedKey = Crypto.getSharedKey(myPrivateKey, theirPublicKey);
        byte[] data = Crypto.aesGCMEncrypt(plaintext, sharedKey);
        return new AnonymouslyEncryptedData(data, myPublicKey);
    }

    public static AnonymouslyEncryptedData readEncryptedData(ByteBuffer buffer, int length, int maxLength)
            throws ConchException.NotValidException {
        if (length > maxLength) {
            throw new ConchException.NotValidException("Max encrypted data length exceeded: " + length);
        }
        byte[] data = new byte[length];
        buffer.get(data);
        byte[] publicKey = new byte[32];
        buffer.get(publicKey);
        return new AnonymouslyEncryptedData(data, publicKey);
    }

    public static AnonymouslyEncryptedData readEncryptedData(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            return readEncryptedData(buffer, bytes.length - 32, Integer.MAX_VALUE);
        } catch (ConchException.NotValidException e) {
            throw new RuntimeException(e.toString(), e); // never
        }
    }

    private final byte[] data;
    private final byte[] publicKey;

    public AnonymouslyEncryptedData(byte[] data, byte[] publicKey) {
        this.data = data;
        this.publicKey = publicKey;
    }

    public byte[] decrypt(String secretPhrase) {
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(secretPhrase), publicKey);
        return Crypto.aesGCMDecrypt(data, sharedKey);
    }

    public byte[] decrypt(byte[] keySeed, byte[] theirPublicKey) {
        if (!Arrays.equals(Crypto.getPublicKey(keySeed), publicKey)) {
            throw new RuntimeException("Data was not encrypted using this keySeed");
        }
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(keySeed), theirPublicKey);
        return Crypto.aesGCMDecrypt(data, sharedKey);
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public int getSize() {
        return data.length + 32;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 32);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data);
        buffer.put(publicKey);
        return buffer.array();
    }

    @Override
    public String toString() {
        return "data: " + Convert.toHexString(data) + " publicKey: " + Convert.toHexString(publicKey);
    }

}
