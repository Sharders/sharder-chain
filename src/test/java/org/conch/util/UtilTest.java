package org.conch.util;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-29
 */
public class UtilTest {
    public static void byteBufferTest(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(80);
        byteBuffer.putInt(2);
        byteBuffer.putLong(33L);
        byte[] byteArray = byteBuffer.array();
        ByteBuffer bufferCp1 = ByteBuffer.allocate(byteArray.length);
        ByteBuffer bufferCp2 = ByteBuffer.allocate(byteArray.length);
        bufferCp1.put(byteArray);
        bufferCp2.put(byteArray);
        System.out.println(Arrays.toString(byteArray));
        System.out.println(Arrays.toString(bufferCp1.array()));
        System.out.println(Arrays.toString(bufferCp2.array()));
    }


    public static void main(String[] args) {
        byteBufferTest();
    }
}
