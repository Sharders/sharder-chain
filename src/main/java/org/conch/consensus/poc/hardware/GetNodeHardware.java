package org.conch.consensus.poc.hardware;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.mint.Generator;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.hyperic.sigar.*;
import sun.net.util.IPAddressUtil;

import java.io.IOException;
import java.net.ConnectException;
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

    public static final int DISK_UNIT_TYPE_KB = 0;
    public static final int DISK_UNIT_TYPE_GB = 1;
    public static final int DISK_UNIT_TYPE_TB = 2;
    public static final int DISK_UNIT_TYPE_PB = 3;

    /**
     * value -> specified unit
     * @param val
     * @param unitType
     * @return
     */
    private static long convertToUnit(long val, int unitType){
        switch(unitType){
            case DISK_UNIT_TYPE_GB:
                return val / 1024L / 1024L;
            case DISK_UNIT_TYPE_TB:
                return val  / 1024L / 1024L / 1024L;
            case DISK_UNIT_TYPE_PB:
                return val  / 1024L / 1024L / 1024L / 1024L;
            default:
                return val;
        }
    }

    public static SystemInfo cpu(SystemInfo systemInfo) throws SigarException {
        Sigar sigar = new Sigar();
        CpuInfo infos[] = sigar.getCpuInfoList();
        int i, count = 0;
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
        systemInfo.setMemoryTotal(convertToUnit(mem.getTotal(), DISK_UNIT_TYPE_GB));
        return systemInfo;
    }

    /**
     * disk capacity by specified unit
     * @param unitType unit type
     * @return
     */
    public static long diskCapacity(int unitType){
        Sigar sigar = new Sigar();
        long capacity = -1;
        try{
            FileSystem fsList[] = sigar.getFileSystemList();
            Long diskTotal = 0L;
            for (int i = 0; i < fsList.length; i++) {
                FileSystem fs = fsList[i];
                FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
                switch (fs.getType()) {
                    case FileSystem.TYPE_UNKNOWN: //未知
                        break;
                    case FileSystem.TYPE_NONE: //TYPE_NONE
                        break;
                    case FileSystem.TYPE_LOCAL_DISK://本地硬盘
                        // 文件系统总大小
                        diskTotal += usage.getTotal();
                        break;
                    case FileSystem.TYPE_NETWORK://网络
                        break;
                    case FileSystem.TYPE_RAM_DISK: //闪存
                        break;
                    case FileSystem.TYPE_CDROM://光驱
                        break;
                    case FileSystem.TYPE_SWAP: //页面交换
                        break;
                    default:
                        break;
                }
            }

            // processing by OS
            if (diskTotal == 0) {
                for (int i = 0; i < fsList.length; i++) {
                    FileSystem fs = fsList[i];
                    if (fs.getDirName().equals("/") //linux
                            || fs.getDirName().equals("/System/Volumes/Data")// OSX
                    ) {
                        FileSystemUsage usage = null;
                        try {
                            usage = sigar.getFileSystemUsage(fs.getDirName());
                        } catch (SigarException e) {
                            e.printStackTrace();
                        }
                        diskTotal += usage.getTotal();
                        break;
                    }
                }
            }

            // convert to specified unit type
            capacity = convertToUnit(diskTotal, unitType);
        }catch(Exception e){
            e.printStackTrace();
        }
        return capacity;
    }

    public static SystemInfo disk(SystemInfo systemInfo) {
        return systemInfo.setHardDiskSize(diskCapacity(DISK_UNIT_TYPE_KB));
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

    public static SystemInfo txPerformance(SystemInfo systemInfo, Integer executeTime) throws Exception {
        systemInfo.setTradePerformance(PerformanceCheckingUtil.check(executeTime));
        return systemInfo;
    }

    public static SystemInfo openingServices(SystemInfo systemInfo) throws Exception {
        List<Peer.Service> services = Peers.getServices();
        Long[] serviceList = new Long[services.size()];

        for (int i = 0; i < services.size(); i++) {
            serviceList[i] = services.get(i).getCode();
        }
        systemInfo.setOpenServices(serviceList);
        return systemInfo;
    }

    private static final String NODE_CONFIG_REPORT_URL = UrlManager.getFoundationUrl(
            UrlManager.NODE_CONFIG_REPORT_EOLINKER,
            UrlManager.NODE_CONFIG_REPORT_LOCAL,
            UrlManager.NODE_CONFIG_REPORT_PATH
    );

    /**
     * 每次开机时，获取节点配置，并主动汇报，以及更新绑定用户
     * <p>
     * 汇报前需要检查是否使用了穿透服务（NAT）
     * 若使用了NAT，则address设置为穿透服务的。
     * 若没有使用NAT,则address设置为本机地址
     *
     * @param executeTime 性能测试时间，单位秒, 为空使用默认值
     * @return true成功，false失败
     */
    public static boolean readAndReport(Integer executeTime) {
        try {
            Logger.logDebugMessage("report the node configuration performance infos to sharder foundation[" + NODE_CONFIG_REPORT_URL + "] ===>");
            return report(calTxPerformance(executeTime));
        } catch (ConchException.NotValidException e) {
            Logger.logWarningMessage(String.format("<=== failed to report configuration performance[caused by %s], maybe Hub isn't initialized yet", e.getMessage()));
        } catch (Exception e) {
            Logger.logErrorMessage("<=== failed to report configuration performance, local error", e);
        }
        return false;
    }

    public static SystemInfo calTxPerformance(Integer executeTime) throws Exception {
        String myAddress = Optional.ofNullable(Conch.getMyAddress())
                .orElseThrow(() -> new ConchException.NotValidException("Current Hub's myAddress is null"));
        // nat service: open - myAddress should be proxy address; nat service : close - myAddress should be public address
        String host = Conch.addressHost(myAddress);
        int port = Conch.addressPort(myAddress);
        String bindRs = Optional.ofNullable(Generator.getAutoMiningRS())
                .orElseThrow(() -> new ConchException.NotValidException("Current Hub's linked MW address is null"));

        if (StringUtils.isEmpty(Conch.getNodeType())) {
            //don't report
            return null;
        }

        SystemInfo systemInfo = readSystemInfo();

        systemInfo.setIp(host).setPort(Integer.toString(port)).setAddress(host)
                .setBindRs(bindRs).setNetworkType(Conch.getNetworkType()).setNodeType(Conch.getNodeType());

        Logger.logDebugMessage("============== Now start testing configuration performance... ==============");
        txPerformance(systemInfo, executeTime);
        Logger.logDebugMessage("============== The configuration performance test is completed ==============");

        Conch.systemInfo = systemInfo;
        return systemInfo;
    }

    public static SystemInfo readSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();
        try{
            cpu(systemInfo);
            memory(systemInfo);
            disk(systemInfo);
            network(systemInfo);
            openingServices(systemInfo);
            Conch.systemInfo = systemInfo;
        }catch(Exception e){
            e.printStackTrace();
        }

        return systemInfo;
    }

    /**
     * 报告节点配置
     *
     * @param systemInfo 性能信息
     * @return true报告成功，false失败
     * @throws IOException 请求异常
     */
    public static Boolean report(SystemInfo systemInfo) {
        try{
            if(systemInfo == null) return false;

            RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(NODE_CONFIG_REPORT_URL)
                    .post()
                    .body(systemInfo)
                    .request();
            boolean result = JSONObject.parseObject(response.getContent()).getBooleanValue(Constants.SUCCESS);
            if (result) {
                Logger.logInfoMessage("<=== Your configuration performance was successfully reported");
                return true;
            } else {
                Logger.logErrorMessage("<=== failed to report configuration performance");
                return false;
            }
        }catch(ConnectException e){
            if (Logger.printNow(Constants.GetNodeHardware_P_report)) {
                Logger.logDebugMessage("connection refused[" + NODE_CONFIG_REPORT_URL + "]");
            }
            return false;
        }catch(Exception e){
            if (Logger.printNow(Constants.GetNodeHardware_P_report)) {
                Logger.logErrorMessage("unknown exception[" + NODE_CONFIG_REPORT_URL + "]", e);
            }
            return false;
        }
    }
}