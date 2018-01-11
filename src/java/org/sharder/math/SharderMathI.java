package org.sharder.math;


/**
 * Sharder相关数学计算
 * @author x.y
 * @date 2017/12/13
 */
public interface SharderMathI {

    /**
     * 成功取回存储数据的几率计算
     * @param boxerCount 网络中的存储者数量
     * @param pieceCount 数据豆的分片数量
     * @param replicaCount 数据豆的备份数量
     * @return 成功取回的几率
     */
    double retrieve(int boxerCount, int pieceCount, int replicaCount);
}
