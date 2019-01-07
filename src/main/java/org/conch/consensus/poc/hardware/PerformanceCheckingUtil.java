package org.conch.consensus.poc.hardware;

/**
 * @ClassName PerformanceCheckingUtil
 * @Description 性能测试类
 * @Version 1.0
 **/
public class PerformanceCheckingUtil {

     /**
      *@Description 根据当前时间计算平方根
      *@Param  执行时间(单位秒)
      *@Return  执行次数
      **/
    public static Long check(Integer time){
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time * 1000;
        long index = 0;
        while(true){
            Math.sqrt(index);
            if (System.currentTimeMillis() > endTime) break;
            index++;
        }
        return index;
    }
}
