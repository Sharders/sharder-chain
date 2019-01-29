package org.conch.consensus.poc.hardware;


/**
 * 性能测试类
 *
 * @author sorayu
 */
public class PerformanceCheckingUtil {

    /**
     * 根据当前时间计算平方根
     *
     * @param time : 执行时间（秒）
     * @return : java.lang.Long 执行的总次数
     * @author : sorayu
     * @date : 2019/1/22 11:26
     */
    public static Long check(Integer time) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time * 1000;
        long index = 0;
        while (true) {
            Math.sqrt(index);
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            index++;
        }
        return index;
    }
}
