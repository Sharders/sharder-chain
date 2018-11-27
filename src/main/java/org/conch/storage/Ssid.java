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

package org.conch.storage;

import io.ipfs.multibase.Base58;

import java.util.Map;
import java.util.TreeMap;

public class Ssid {

    public Ssid() {
    }

    public static String encode(Type type, String data) {
          return type.prefix + Base58.encode(data.getBytes());
    }

    public static Type encoding(String data) {
        return Type.lookup(data.charAt(0));
    }

    public static String decode(String data) {
        String rest = data.substring(1);
        return new String(Base58.decode(rest));
    }

    //TODO impl more store method: OSS, Cloud, Local Disk
    public static enum Type {
        Local('l'),
        IPFS('i');

        public char prefix;
        private static Map<Character, Type> lookup = new TreeMap();

        private Type(char prefix) {
            this.prefix = prefix;
        }

        public static Type lookup(char p) {
            if (!lookup.containsKey(p)) {
                throw new IllegalStateException("Unknown SSID type: " + p);
            } else {
                return (Type)lookup.get(p);
            }
        }

        static {
            Type[] var0 = values();
            int var1 = var0.length;

            for(int var2 = 0; var2 < var1; ++var2) {
                Type b = var0[var2];
                lookup.put(b.prefix, b);
            }

        }
    }
}
