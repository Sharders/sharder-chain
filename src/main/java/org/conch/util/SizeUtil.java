package org.conch.util;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public class SizeUtil {
    //long 8
    //int 4
    //short 2
    //byte 2
   public static int sizeOf(Object obj){
       if(obj instanceof Long) return 16;
       if(obj instanceof Integer) return 16;
       if(obj instanceof Byte[]) return ((Byte[]) obj).length;
       if(obj instanceof String) return ((String) obj).length();
       if(obj instanceof Boolean) return 4;
       return 0;
   }

  public static void main(String[] args) {
    Long tt = 2L;
    System.out.println(sizeOf(tt));
  }
}
