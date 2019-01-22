package org.conch.consensus.poc.hardware;

import com.alibaba.fastjson.JSONObject;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.RestfulHttpClient;
import org.hyperic.sigar.*;
import sun.net.util.IPAddressUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        systemInfo.setCore(infos.length).setAverageMHz(count / infos.length);
        return systemInfo;
    }

    public static SystemInfo memory(SystemInfo systemInfo) throws SigarException {
        Sigar sigar = new Sigar();
        Mem mem = sigar.getMem();
        systemInfo.setMemoryTotal((int)(mem.getTotal() / 1024 / 1024 / 1024));
        return systemInfo;
    }

    public static SystemInfo disk(SystemInfo systemInfo) throws Exception {
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
        int hdTotal = (int)((double)ypTotal / 1024L / 1024L);
        if (hdTotal == 0){
            for (int i = 0; i <fsList.length; i++) {
                FileSystem fs = fsList[i];
                if (fs.getDirName().equals("/")) {
                    FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
                     hdTotal = (int)((double)usage.getTotal() / 1024L / 1024L);
                     break;
                }
            }
        }
        systemInfo.setHardDiskSize(hdTotal);
        return systemInfo;
    }

    private static boolean externalIp(String ip) {
        byte[] addr = IPAddressUtil.textToNumericFormatV4(ip);
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        //10.x.x.x/8
        final byte section1 = 0x0A;
        //172.16.x.x/12
        final byte section2 = (byte) 0xAC;
        final byte section3 = (byte) 0x10;
        final byte section4 = (byte) 0x1F;
        //192.168.x.x/16
        final byte section5 = (byte) 0xC0;
        final byte section6 = (byte) 0xA8;
        switch (b0) {
        case section1:
            return false;
        case section2:
            if (b1 >= section3 && b1 <= section4) {
                return false;
            }
        case section5:
            switch (b1) {
            case section6:
                return false;
            }
        default:
            return true;
        }
    }

    public static SystemInfo network(SystemInfo systemInfo) throws Exception {
        Sigar sigar = new Sigar();

        String[] ipExcept = new String[]{"127.0.0.1", "0.0.0.0"};

        String[] netInterfaceList = sigar.getNetInterfaceList();

        Long bandWidth = 0L;

        Boolean hadPublicIp = false;

        // 获取网络流量信息
        for (int i = 0; i < netInterfaceList.length; i++) {
            String netInterface = netInterfaceList[i];// 网络接口
            NetInterfaceConfig netInterfaceConfig = sigar.getNetInterfaceConfig(netInterface);
            if (Arrays.asList(ipExcept).contains(netInterfaceConfig.getAddress()) || (netInterfaceConfig.getFlags() & 1L) <= 0L) {
                continue;
            }
            NetInterfaceStat netInterfaceStat = sigar.getNetInterfaceStat(netInterface);
            bandWidth = netInterfaceStat.getSpeed() / 1000000L / 8;
            hadPublicIp = externalIp(netInterfaceConfig.getAddress());
            break;
        }
        sigar.close();
        systemInfo.setBandWidth(bandWidth.intValue());
        systemInfo.setHadPublicIp(hadPublicIp);
        return systemInfo;
    }
    
    private static final int DEFAULT_TX_CHECKING_COUNT = 1000;
    public static SystemInfo txPerformance(SystemInfo systemInfo) throws Exception {
        systemInfo.setTradePerformance(PerformanceCheckingUtil.check(10));
        return systemInfo;
    }
    
    public static SystemInfo openingServices(SystemInfo systemInfo) throws Exception {
        List<Peer.Service> services = Peers.getServices();
        Long[] serviceList = new Long[services.size()];
        
        for(int i = 0 ; i < services.size(); i++ ){
            serviceList[i] = services.get(i).getCode();
        }
        systemInfo.setOpenServices(serviceList);
        return systemInfo;
    }

    private static String scHardwareApiUrl() {
        if (Constants.isMainnet() || Constants.isTestnet()) {
            return Constants.HTTP + Conch.getSharderFoundationURL() + "/sc/peer/list.ss";
        }

        return "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/sc/peer/report.ss";
    }

    public static final String SYSTEM_INFO_REPORT_URL = scHardwareApiUrl();
    
    public static boolean readAndReport(){
        //提交系统配置信息
        SystemInfo systemInfo = new SystemInfo();
        /*
        汇报前需要检查是否使用了穿透服务（NAT）
        若使用了NAT，则address设置为穿透服务的。
        若没有使用NAT,则address设置为本机地址
         */
        boolean useNat = Conch.getUseNATService();
        String myAddress = Conch.getMyAddress();
        String ip = Optional.ofNullable(Conch.NAT_SERVICE_ADDRESS).orElse(Conch.addressHost(myAddress));
        Integer port = Optional.of(Conch.NAT_SERVICE_PORT).filter(num -> num != 0).orElse(Conch.addressPort(myAddress));
        if (useNat) {
            systemInfo.setIp(ip).setPort(port.toString()).setAddress(ip + ":" + port.toString());
        }
        
        try {
            cpu(systemInfo);
            memory(systemInfo);
            disk(systemInfo);
            network(systemInfo);
            txPerformance(systemInfo);
            openingServices(systemInfo);

            RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(SYSTEM_INFO_REPORT_URL)
                    .post()
                    .body(systemInfo)
                    .request();
            String result = Optional.ofNullable(JSONObject.parseObject(response.getContent()).get(Constants.SUCCESS)).map(Object::toString).orElse("false");
            System.out.println("report the System hardware infos to sharder foundation[" + SYSTEM_INFO_REPORT_URL  + "] ===>");
            System.out.println(systemInfo.toString());
            if (Boolean.TRUE.toString().equalsIgnoreCase(result)) {
                System.out.println("<=== success to report hardware performance");
                return true;
            } else {
                System.out.println("<=== failed to report hardware performance");
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
