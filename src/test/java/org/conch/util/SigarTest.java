package org.conch.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hyperic.sigar.OperatingSystem;

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

    public static void main(String[] args) {
        System.out.println(StringUtils.repeat('=', 10));
        getOsInfo();
        System.out.println(StringUtils.repeat('=', 10));
        getSystemInfo();
        System.out.println(StringUtils.repeat('=', 10));
    }
}
