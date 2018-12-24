package org.conch.util;

/**
 * @ClassName PerformanceTestUtil
 * @Description 性能测试工具类
 * @Version 1.0
 **/
public class PerformanceTestUtil {

     /**
      *@Description 根据当前时间计算平方根
      *@Param  执行时间(单位秒)
      *@Return  执行次数
      **/
    public static Long test(Integer time){
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time * 1000;
        long index = 0;
        while(true){
            Math.sqrt(index);
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            index++;
        }
        return index;
    }
}
