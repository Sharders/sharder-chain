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

public enum HashFunction {

    /**
     * Use Java implementation of SHA256 (code 2)
     */
    SHA256((byte)2) {
        public byte[] hash(byte[] input) {
            return Crypto.sha256().digest(input);
        }
    },
    /**
     * Use Bouncy Castle implementation of SHA3 (code 3). As of Bouncy Castle 1.53, this has been renamed to Keccak.
     */
    SHA3((byte)3) {
        public byte[] hash(byte[] input) {
            return Crypto.sha3().digest(input);
        }
    },
    /**
     * Use Java implementation of Scrypt
     */
    SCRYPT((byte)5) {
        public byte[] hash(byte[] input) {
            return threadLocalScrypt.get().hash(input);
        }
    },
    /**
     * Use proprietary Conch implementation of Keccak with 25 rounds (code 25)
     */
    Keccak25((byte)25) {
        public byte[] hash(byte[] input) {
            return KNV25.hash(input);
        }
    },
    RIPEMD160((byte)6) {
        public byte[] hash(byte[] input) {
            return Crypto.ripemd160().digest(input);
        }
    },
    RIPEMD160_SHA256((byte)62) {
        public byte[] hash(byte[] input) {
            return Crypto.ripemd160().digest(Crypto.sha256().digest(input));
        }
    };

    private static final ThreadLocal<Scrypt> threadLocalScrypt = new ThreadLocal<Scrypt>() {
        @Override
        protected Scrypt initialValue() {
            return new Scrypt();
        }
    };

    private final byte id;

    HashFunction(byte id) {
        this.id = id;
    }

    public static HashFunction getHashFunction(byte id) {
        for (HashFunction function : values()) {
            if (function.id == id) {
                return function;
            }
        }
        throw new IllegalArgumentException(String.format("illegal algorithm %d", id));
    }

    public byte getId() {
        return id;
    }

    public abstract byte[] hash(byte[] input);
}
