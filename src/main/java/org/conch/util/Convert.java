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

package org.conch.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.conch.common.ConchException;
import org.conch.common.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class Convert {

    private static final char[] hexChars = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
    private static final long[] multipliers = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    public static final BigInteger two64 = new BigInteger("18446744073709551616");
    public static final long[] EMPTY_LONG = new long[0];
    public static final byte[] EMPTY_BYTE = new byte[0];
    public static final byte[][] EMPTY_BYTES = new byte[0][];
    public static final String[] EMPTY_STRING = new String[0];

    private Convert() {} //never

    public static byte[] parseHexString(String hex) {
        if (hex == null) {
            return null;
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int char1 = hex.charAt(i * 2);
            char1 = char1 > 0x60 ? char1 - 0x57 : char1 - 0x30;
            int char2 = hex.charAt(i * 2 + 1);
            char2 = char2 > 0x60 ? char2 - 0x57 : char2 - 0x30;
            if (char1 < 0 || char2 < 0 || char1 > 15 || char2 > 15) {
                throw new NumberFormatException("Invalid hex number: " + hex);
            }
            bytes[i] = (byte)((char1 << 4) + char2);
        }
        return bytes;
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = hexChars[((bytes[i] >> 4) & 0xF)];
            chars[i * 2 + 1] = hexChars[(bytes[i] & 0xF)];
        }
        return String.valueOf(chars);
    }

    public static long parseUnsignedLong(String number) {
        if (number == null) {
            return 0;
        }
        return Long.parseUnsignedLong(number);
    }

    public static long parseLong(Object o) {
        if (o == null) {
            return 0;
        } else if (o instanceof Long) {
            return ((Long)o);
        } else if (o instanceof String) {
            return Long.parseLong((String)o);
        } else {
            throw new IllegalArgumentException("Not a long: " + o);
        }
    }


    public static long fullHashToId(byte[] hash) {
        if (hash == null || hash.length < 8) {
            throw new IllegalArgumentException("Invalid hash: " + Arrays.toString(hash));
        }
        BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
        return bigInteger.longValue();
    }

    public static long fromEpochTime(int epochTime) {
        return epochTime * 1000L + Constants.EPOCH_BEGINNING - 500L;
    }

    public static int toEpochTime(long currentTime) {
        return (int)((currentTime - Constants.EPOCH_BEGINNING + 500) / 1000);
    }

    public static String emptyToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static byte[] emptyToNull(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        for (byte b : bytes) {
            if (b != 0) {
                return bytes;
            }
        }
        return null;
    }

    public static byte[][] nullToEmpty(byte[][] bytes) {
        return bytes == null ? EMPTY_BYTES : bytes;
    }

    public static long[] nullToEmpty(long[] array) {
        return array == null ? EMPTY_LONG : array;
    }

    public static long nullToZero(Long l) {
        return l == null ? 0 : l;
    }

    public static long[] toArray(List<Long> list) {
        long[] result = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    public static List<Long> toList(long[] array) {
        List<Long> result = new ArrayList<>(array.length);
        for (long elem : array) {
            result.add(elem);
        }
        return result;
    }

    public static Long[] toArray(long[] array) {
        Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static long[] toArray(Long[] array) {
        long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Set<Long> toSet(long[] array) {
        if (array == null || array.length ==0) {
            return Collections.emptySet();
        }
        Set<Long> set = new HashSet<>(array.length);
        for (long elem : array) {
            set.add(elem);
        }
        return set;
    }

    public static byte[] toBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static byte[] toBytes(String s, boolean isText) {
        return isText ? toBytes(s) : parseHexString(s);
    }

    public static String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static String toString(byte[] bytes, boolean isText) {
        return isText ? toString(bytes) : toHexString(bytes);
    }

    public static byte[] toBytes(long n) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte)(n >> (8 * i));
        }
        return bytes;
    }

    public static int countJsonBytes(Object obj){
        return toBytes(JSONObject.toJSONString(obj)).length;
    }

    public static String readString(ByteBuffer buffer, int numBytes, int maxLength) throws ConchException.NotValidException {
        if (numBytes > 3 * maxLength) {
            throw new ConchException.NotValidException("Max parameter length exceeded");
        }
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return Convert.toString(bytes);
    }
    
    public static int writeString(ByteBuffer buffer, String content) {
        byte[] contentBytes = toBytes(content);
        buffer.putInt(contentBytes.length);
        buffer.put(contentBytes);
        return contentBytes.length;
    }
    
    public static int writeMap(ByteBuffer buffer, Map map) {
        return writeString(buffer,JSONObject.toJSONString(map));
    }
    
    public static int writeList(ByteBuffer buffer, List list) {
        return writeString(buffer,JSONObject.toJSONString(list));
    }
    
    public static int writeMapArray(ByteBuffer buffer, List<Map> mapArray) {
        int size = 0;
        if(mapArray == null || mapArray.size() <= 0) return size;
        
        for(Map map : mapArray){
            size += writeString(buffer,JSONObject.toJSONString(map));
        }
        return size;
    }

    public static String truncate(String s, String replaceNull, int limit, boolean dots) {
        return s == null ? replaceNull : s.length() > limit ? (s.substring(0, dots ? limit - 3 : limit) + (dots ? "..." : "")) : s;
    }

    public static long parseSS(String ss) {
        return parseStringFraction(ss, 8, Constants.MAX_BALANCE_SS);
    }

    private static long parseStringFraction(String value, int decimals, long maxValue) {
        String[] s = value.trim().split("\\.");
        if (s.length == 0 || s.length > 2) {
            throw new NumberFormatException("Invalid number: " + value);
        }
        long wholePart = Long.parseLong(s[0]);
        if (wholePart > maxValue) {
            throw new IllegalArgumentException("Whole part of value exceeds maximum possible");
        }
        if (s.length == 1) {
            return wholePart * multipliers[decimals];
        }
        long fractionalPart = Long.parseLong(s[1]);
        if (fractionalPart >= multipliers[decimals] || s[1].length() > decimals) {
            throw new IllegalArgumentException("Fractional part exceeds maximum allowed divisibility");
        }
        for (int i = s[1].length(); i < decimals; i++) {
            fractionalPart *= 10;
        }
        return wholePart * multipliers[decimals] + fractionalPart;
    }

    public static byte[] compress(byte[] bytes) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(bytes);
            gzip.flush();
            gzip.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] uncompress(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int nRead;
            while ((nRead = gzip.read(buffer, 0, buffer.length)) > 0) {
                bos.write(buffer, 0, nRead);
            }
            bos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static final Comparator<byte[]> byteArrayComparator = (o1, o2) -> {
        int minLength = Math.min(o1.length, o2.length);
        for (int i = 0; i < minLength; i++) {
            int result = Byte.compare(o1[i], o2[i]);
            if (result != 0) {
                return result;
            }
        }
        return o1.length - o2.length;
    };

    public static String stringTemplate(String formatMsg, Object... objects) {
        return String.format(StringUtils.replace(formatMsg, "{}", "%s"), objects);
    }

}
