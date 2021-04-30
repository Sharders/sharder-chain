package org.conch.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conch.consensus.poc.hardware.GetNodeHardware;
import org.hyperic.sigar.*;

public class SigarTest {

    private static void getSystemInfo() {
        OperatingSystem operatingSystem = OperatingSystem.getInstance();
        System.out.println(SystemUtils.OS_NAME + " " + SystemUtils.OS_ARCH);
        System.out.println("操作系统名：" + operatingSystem.getName());
        System.out.println("操作系统内核类型：" + operatingSystem.getArch());
        System.out.println("操作系统描述：" + operatingSystem.getDescription());
        System.out.println("操作系统版本：" + operatingSystem.getVersion());
        System.out.println("机器：" + operatingSystem.getMachine());
        System.out.println("cpu字节序：" + operatingSystem.getCpuEndian());
        System.out.println("数据模型：" + operatingSystem.getDataModel());
        System.out.println("补丁等级：" + operatingSystem.getPatchLevel());
        System.out.println("出售厂家：" + operatingSystem.getVendor());
        System.out.println("出售厂家代码：" + operatingSystem.getVendorCodeName());
        System.out.println("出售厂家版本：" + operatingSystem.getVendorVersion());

    }

    private static void getOsInfo() {
        System.out.println("是WINDOWS吗？" + SystemUtils.IS_OS_WINDOWS);
        System.out.println("是WINDOWS7吗？" + SystemUtils.IS_OS_WINDOWS_7);
        System.out.println("是WINDOWS8吗？" + SystemUtils.IS_OS_WINDOWS_8);
        System.out.println("是WINDOWS10吗？" + SystemUtils.IS_OS_WINDOWS_10);
        System.out.println("是Linux吗？" + SystemUtils.IS_OS_LINUX);
        System.out.println("是UNIX吗？" + SystemUtils.IS_OS_UNIX);
        System.out.println("是MAC吗？" + SystemUtils.IS_OS_MAC);
        System.out.println("是MAC OSX吗？" + SystemUtils.IS_OS_MAC_OSX);
        System.out.println("是MAC CHEETAH吗？" + SystemUtils.IS_OS_MAC_OSX_CHEETAH);
        System.out.println("是MAC YOSEMITE？" + SystemUtils.IS_OS_MAC_OSX_YOSEMITE);
        System.out.println("是JDK10吗？" + SystemUtils.IS_JAVA_10);
        System.out.println("是JDK9吗？" + SystemUtils.IS_JAVA_9);
        System.out.println("是JDK8吗？" + SystemUtils.IS_JAVA_1_8);
        System.out.println("是JDK7吗？" + SystemUtils.IS_JAVA_1_7);
        System.out.println("是JDK6吗？" + SystemUtils.IS_JAVA_1_6);
        System.out.println("是JDK5吗？" + SystemUtils.IS_JAVA_1_5);
        System.out.println("是JDK4吗？" + SystemUtils.IS_JAVA_1_4);
        System.out.println("主机名：" + SystemUtils.getHostName());
        System.out.println("%JAVA_HOME%：" + SystemUtils.getJavaHome());
        System.out.println("项目路径：" + SystemUtils.getUserDir());
        System.out.println("用户目录：" + SystemUtils.getUserHome());
        System.out.println("操作系统平台：" + SystemUtils.OS_NAME);
        System.out.println("操作系统位数：" + SystemUtils.OS_ARCH);
        System.out.println("操作系统版本：" + SystemUtils.OS_VERSION);
        System.out.println("用户语言：" + SystemUtils.USER_LANGUAGE);

    }

    private static void getDiskInfo() throws SigarException {
        Sigar sigar = new Sigar();

        FileSystem[] fileSystemArray = sigar.getFileSystemList();
        for (FileSystem fileSystem : fileSystemArray) {
            System.out.println("fileSystem dirName：" + fileSystem.getDirName());//分区的盘符名称
            System.out.println("fileSystem devName：" + fileSystem.getDevName());//分区的盘符名称
            System.out.println("fileSystem typeName：" + fileSystem.getTypeName());// 文件系统类型名，比如本地硬盘、光驱、网络文件系统等
            System.out.println("fileSystem sysTypeName：" + fileSystem.getSysTypeName());//文件系统类型，比如 FAT32、NTFS
            System.out.println("fileSystem options：" + fileSystem.getOptions());
            System.out.println("fileSystem flags：" + fileSystem.getFlags());
            System.out.println("fileSystem type：" + fileSystem.getType());

            FileSystemUsage fileSystemUsage = null;

            try {
                fileSystemUsage = sigar.getFileSystemUsage(fileSystem.getDirName());
            } catch (SigarException e) {//当fileSystem.getType()为5时会出现该异常——此时文件系统类型为光驱
                continue;
            }
            System.out.println("fileSystemUsage total：" + fileSystemUsage.getTotal() + "KB");// 文件系统总大小
            System.out.println("fileSystemUsage free：" + fileSystemUsage.getFree() + "KB");// 文件系统剩余大小
            System.out.println("fileSystemUsage used：" + fileSystemUsage.getUsed() + "KB");// 文件系统已使用大小
            System.out.println("fileSystemUsage avail：" + fileSystemUsage.getAvail() + "KB");// 文件系统可用大小
            System.out.println("fileSystemUsage files：" + fileSystemUsage.getFiles());
            System.out.println("fileSystemUsage freeFiles：" + fileSystemUsage.getFreeFiles());
            System.out.println("fileSystemUsage diskReadBytes：" + fileSystemUsage.getDiskReadBytes());
            System.out.println("fileSystemUsage diskWriteBytes：" + fileSystemUsage.getDiskWriteBytes());
            System.out.println("fileSystemUsage diskQueue：" + fileSystemUsage.getDiskQueue());
            System.out.println("fileSystemUsage diskServiceTime：" + fileSystemUsage.getDiskServiceTime());
            System.out.println("fileSystemUsage usePercent：" + fileSystemUsage.getUsePercent() * 100 + "%");// 文件系统资源的利用率
            System.out.println("fileSystemUsage diskReads：" + fileSystemUsage.getDiskReads());
            System.out.println("fileSystemUsage diskWrites：" + fileSystemUsage.getDiskWrites());
//            System.err.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

            try {
                System.out.println(">>DiskSize：" + GetNodeHardware.diskCapacity(GetNodeHardware.DISK_UNIT_TYPE_KB) + " KB");
                System.out.println(">>DiskSize：" + GetNodeHardware.diskCapacity(GetNodeHardware.DISK_UNIT_TYPE_GB) + " GB");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws SigarException {
        System.out.println(StringUtils.repeat('=', 10));
        getOsInfo();
        System.out.println(StringUtils.repeat('=', 10));
        getSystemInfo();
        System.out.println(StringUtils.repeat('=', 10));
        getDiskInfo();
    }
}
