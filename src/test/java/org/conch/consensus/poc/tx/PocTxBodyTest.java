package org.conch.consensus.poc.tx;

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.util.Convert;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/2
 */
public class PocTxBodyTest {
    static ByteBuffer buffer = null;
    static String byteStr = null;
    
    static void defaultTemplate2BytesTest(){
        PocTxBody.PocWeightTable weightTable = PocTxBody.PocWeightTable.defaultPocWeightTable();
        buffer = ByteBuffer.allocate(weightTable.getMySize());
        weightTable.putMyBytes(buffer);
        byteStr = Convert.toString(buffer.array());
        System.out.println("size=" + byteStr.length());
        System.out.println(byteStr);
    }
    
    static void bytes2WeightTableTest() throws ConchException.NotValidException {
        PocTxBody.PocWeightTable weightTable = new PocTxBody.PocWeightTable(ByteBuffer.wrap(Convert.toBytes(byteStr)),(byte)1);
        System.out.println(weightTable.toString());
    }
    
  public static void main(String[] args) throws ConchException.NotValidException {
    //      defaultTemplate2BytesTest();
    //      bytes2WeightTableTest();
    System.out.println(Conch.getEpochTime());
  }
}
