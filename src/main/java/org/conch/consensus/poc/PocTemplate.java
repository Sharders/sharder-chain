package org.conch.consensus.poc;

import java.io.Serializable;
import java.math.BigInteger;

/**********************************************************************************
 * @package org.conch.consensus.poc
 * @author Wolf Tian
 * @email twenbin@sharder.org
 * @company Sharder Foundation
 * @website https://www.sharder.org/
 * @creatAt 2018-Nov-28 11:52 Wed
 * @tel 18716387615
 * @comment
 **********************************************************************************/
public class PocTemplate implements Serializable {
    private static final long serialVersionUID = 8307320307767369807L;

    public enum TypeState {
        FOUNDATION_NODE, COMMUNITY_NODE, HUB_NODE, BOX_NODE, COMMON_NODE
    }

    public enum OnLineState {
        // 基金会节点 在线率
        FOUNDATION_ONLINE_LEVEL1, // 99.00%<在线率<99.99% ， 得分 -2
        FOUNDATION_ONLINE_LEVEL2, // 97.00%<在线率<99.00% ， 得分 -5
        FOUNDATION_ONLINE_LEVEL3, // 在线率<97.00% ， 得分 -10

        // 社区节点 在线率
        COMMUNITY_ONLINE_LEVEL1, // 97.00%<在线率<99.00% ，得分 -2
        COMMUNITY_ONLINE_LEVEL2, // 90.00%<在线率<97.00%， 得分 -5
        COMMUNITY_ONLINE_LEVEL3, // 在线率<90.00%， 得分 -10

        // HUB / BOX 节点 在线率
        HUB_BOX_ONLINE_LEVEL1, // 在线率<90.00% ， 得分 -5

        // 普通节点 在线率
        COMMON_ONLINE_LEVEL1 // 在线率>90.00% ， 得分 +5
    }

    public enum NetState {
        NO_PUBLIC_IP, // 极差:无公网 IP
        PUBLIC_IP_1_5MBPS, // 差:有公网 IP，1-5Mbps
        PUBLIC_IP_5_10MBPS, //  中: 有公网 IP，5-10Mbps
        PUBLIC_IP_10MBPS_PLUS //  好: 有公网 IP，10+Mbps
    }

    public enum TPPState { // 交易处理性能
        TPS_100_300, // 低: 100-300 TPS
        TPS_500_1000, // 中: 500-1000 TPS
        TPS_1000_PLUS, // 高: 1000+ TPS
    }

    public enum  BMState { // blocking miss
        ZERO, // 零
        PERMONTH_1BLOCK_TOTAL_LESS3, // 低: 1块/月 累积丢失数小于 3
        PERWEEK_LESS_3BLOCK_TOTAL_LESS10, // 中: 小于3块/周 累积丢失数小于 10
        PERWEEK_GREATER_3BLOCK_TOTAL_GREATER10 // 高: 大于3块每周 累积丢失数大于 10
    }

    public enum RBCState { // 分叉收敛速度 Rate of bifurcation convergence
        HARD_BIFURCATION, // 硬分叉
        GREATER_10BLOCK, // 慢: 超过10块
        LESS_5BLOCK, // 中: 小于5块
        LESS_2BLOCK // 快: 小于2块
    }

    public static class DeviceConfig {

        public enum CpuState { // CPU配置
            CORES2_THREADS4_24GHZ_PLUS, // 2核4线程2.4GHz+
            CORES4_THREADS4_31GHZ_PLUS, // 4核4线程3.1GHz+
            CORES8_THREADS8_36GHZ_PLUS // 8核8线程3.6GHz+
        }

        public enum MemoryState { // 内存配置
            DDR3_4G_PLUS, // DDR3 4G+
            DDR4_8G_PLUS, // DDR4 8G+
            DDR4_16G_PLUS, // DDR4 16G+
        }

        public enum HardDiskState { // 硬盘配置
            R5400_100GB, // 5400转 100GB
            R5400_1TB_PLUS, // 5400转 1TB+
            R7200_PLUS_10TB_PLUS // 7200转+ 10TB+
        }

        public enum DeviceState {
            LEVEL_LOW(CpuState.CORES2_THREADS4_24GHZ_PLUS, MemoryState.DDR3_4G_PLUS, HardDiskState.R5400_100GB), // 低: CPU 2核4线程 2.4GHz+;内存 DDR3 4G+; 硬盘 5400 转 100GB
            LEVEL_MIDDLE(CpuState.CORES4_THREADS4_31GHZ_PLUS, MemoryState.DDR4_8G_PLUS, HardDiskState.R5400_1TB_PLUS), // 中: CPU 4核4线程 3.1GHz+; 内存 DDR4 8G+; 硬盘 5400 转 1T+
            LEVEL_HIGH(CpuState.CORES8_THREADS8_36GHZ_PLUS, MemoryState.DDR4_16G_PLUS, HardDiskState.R7200_PLUS_10TB_PLUS); // 高: CPU 8核8线程 3.6GHz+;内存 DDR4 16G+; 硬盘 7200+转 10T+

            private CpuState cpu;
            private MemoryState memory;
            private HardDiskState hardDisk;

            public CpuState getCpu() {
                return cpu;
            }

            public void setCpu(CpuState cpu) {
                this.cpu = cpu;
            }

            public MemoryState getMemory() {
                return memory;
            }

            public void setMemory(MemoryState memory) {
                this.memory = memory;
            }

            public HardDiskState getHardDisk() {
                return hardDisk;
            }

            public void setHardDisk(HardDiskState hardDisk) {
                this.hardDisk = hardDisk;
            }

            private DeviceState(CpuState cpu, MemoryState memory, HardDiskState hardDisk) {
                this.cpu = cpu;
                this.memory = memory;
                this.hardDisk = hardDisk;
            }
        }

    }


}
