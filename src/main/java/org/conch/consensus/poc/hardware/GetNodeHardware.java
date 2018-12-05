package org.conch.consensus.poc.hardware;

import org.conch.util.SendHttpRequest;
import org.hyperic.sigar.*;

/**
 * @ClassName GetNodeHardware
 * @Description
 * @Author 栗子
 * @Version 1.0
 **/
public class GetNodeHardware {

    public static SystemInfo cpu(SystemInfo systemInfo) throws SigarException {
        Sigar sigar = new Sigar();
        CpuInfo infos[] = sigar.getCpuInfoList();
        int i,count = 0;
        for (i = 0; i < infos.length; i++) {
            CpuInfo info = infos[i];
            count += info.getMhz();
        }
        systemInfo.setCore(infos.length);
        systemInfo.setAverageMHz(count / infos.length);
        return systemInfo;
    }

    public static SystemInfo memory(SystemInfo systemInfo) throws SigarException {
        Sigar sigar = new Sigar();
        Mem mem = sigar.getMem();
        systemInfo.setMemoryTotal((int)(mem.getTotal() / 1024 / 1024 / 1024));
        return systemInfo;
    }

    public static SystemInfo file(SystemInfo systemInfo) throws Exception {
        Sigar sigar = new Sigar();
        FileSystem fsList[] = sigar.getFileSystemList();
        Long ypTotal = 0L;
        for (int i = 0; i < fsList.length; i++) {
            FileSystem fs = fsList[i];
            FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
            switch (fs.getType()) {
                case 0: // TYPE_UNKNOWN ：未知
                    break;
                case 1: // TYPE_NONE
                    break;
                case 2: // TYPE_LOCAL_DISK : 本地硬盘
                    // 文件系统总大小
                    ypTotal += usage.getTotal();
                    break;
                case 3:// TYPE_NETWORK ：网络
                    break;
                case 4:// TYPE_RAM_DISK ：闪存
                    break;
                case 5:// TYPE_CDROM ：光驱
                    break;
                case 6:// TYPE_SWAP ：页面交换
                    break;
            }
        }
        int hdTotal = (int)((double)ypTotal / (1024L * 1024L));
        systemInfo.setHardDiskSize(hdTotal);
        return systemInfo;
    }

    public static final String SYSTEM_INFO_REPORT_URL = "http://192.168.31.5:8080/bounties/SC/report";
    public static void readAndPush(){
        //提交系统配置信息
        SystemInfo systemInfo = new SystemInfo();
        try {
            cpu(systemInfo);
            memory(systemInfo);
            file(systemInfo);
            System.out.println(systemInfo.toString());
            SendHttpRequest.sendPost(SYSTEM_INFO_REPORT_URL,"test");
//            SendHttpRequest.sendPost(SYSTEM_INFO_REPORT_URL,JSON.toJSONString(systemInfo));
            System.out.println("------------------------系统信息-------------------------");
            System.out.println(systemInfo.getCore());
            System.out.println(systemInfo.getAverageMHz());
            System.out.println(systemInfo.getHardDiskSize());
            System.out.println(systemInfo.getMemoryTotal());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
