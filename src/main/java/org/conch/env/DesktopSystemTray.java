/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.env;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.common.Constants;
import org.conch.mint.Generator;
import org.conch.peer.Peers;
import org.conch.util.Convert;
import org.conch.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DesktopSystemTray {

    public static final int DELAY = 1000;

    private SystemTray tray;
    private final JFrame wrapper = new JFrame();
    private JDialog statusDialog;
    private JPanel statusPanel;
    private ImageIcon imageIcon;
    private TrayIcon trayIcon;
    private MenuItem openWalletInBrowser;
    private MenuItem viewLog;
    private SystemTrayDataProvider dataProvider;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM, Locale.getDefault());

    class ItemName {
        String en;
        String cn;

        public ItemName(String en,String cn){
            this.en = en;
            this.cn = cn;
        }
    }

    private static final boolean inCN = false;
    private static String getItemDisplay(String key){
        if(!itemNameMap.containsKey(key)) return key;
        return inCN ? itemNameMap.get(key).cn : itemNameMap.get(key).en;
    }

    private static String getNetwork(){
        return Constants.isOffline ? "Offline Mode" : getItemDisplay(Constants.getNetwork().getName());
    }

    // support cn and en languages
    private static final Map<String,ItemName> itemNameMap = new ConcurrentHashMap<>();
    {
        itemNameMap.put("Shutdown",new ItemName("Shutdown","关闭"));
        itemNameMap.put("OpenWIB",new ItemName("Open in Browser","浏览器端"));
        itemNameMap.put("ShowDA",new ItemName("Show Client","显示终端"));
        itemNameMap.put("RefreshW",new ItemName("Refresh Client","刷新终端"));
        itemNameMap.put("ViewLF",new ItemName("View Log File","查看日志"));
        itemNameMap.put("Status",new ItemName("Status","终端状态"));
        itemNameMap.put("Initializing",new ItemName("Initializing","初始化中"));
        itemNameMap.put("ShutdownTips",new ItemName("Sure you want to shutdown COS?\n\nIf you do, this will stop mining.\n\n","你确定要关闭COS吗?\n\n关闭将会导致无法挖矿!\n\n"));
        itemNameMap.put("Installation",new ItemName("Client","客户端"));
        itemNameMap.put("Application",new ItemName("Application","应用程序"));
        itemNameMap.put("Network",new ItemName("Network","网络"));
        itemNameMap.put("WorkingO",new ItemName("Offline","离线模式"));
        itemNameMap.put("Browser",new ItemName("Browser","浏览器端"));
        itemNameMap.put("PeerP",new ItemName("Peer port","监听端口"));
        itemNameMap.put("ProgramF",new ItemName("Program folder","程序目录"));
        itemNameMap.put("UserF",new ItemName("Config folder","用户目录"));
        itemNameMap.put("DatabaseU",new ItemName("Database URL","数据库"));
        itemNameMap.put("LastB",new ItemName("Block","区块"));
        itemNameMap.put("Height",new ItemName("Height","区块高度"));
        itemNameMap.put("Timestamp",new ItemName("Timestamp","时间戳"));
        itemNameMap.put("Time",new ItemName("Last Block","最后区块"));
        itemNameMap.put("SecondsP",new ItemName("Seconds passed","过去秒数"));
        itemNameMap.put("Forging",new ItemName("Mining","开启挖矿"));
        itemNameMap.put("ForgingA",new ItemName("Mining accounts","挖矿账户"));
        itemNameMap.put("Environment",new ItemName("Environment","环境信息"));
        itemNameMap.put("NumberOP",new ItemName("Number of peers","节点数"));
        itemNameMap.put("Unavailable",new ItemName("Unavailable","不可用"));
        itemNameMap.put("DevNet", new ItemName("Devnet", "开发网络"));
        itemNameMap.put("TestNet", new ItemName("Testnet", "测试网络"));
        itemNameMap.put("MainNet", new ItemName("Mainnet", "正式网络"));
        itemNameMap.put("AvailableP",new ItemName("Available miners","矿工"));
        itemNameMap.put("MaxM",new ItemName("Max memory","最大内存"));
        itemNameMap.put("TotalM",new ItemName("Total memory","占用内存"));
        itemNameMap.put("FreeM",new ItemName("Free memory","空闲内存"));
        itemNameMap.put("ProcessI",new ItemName("Process id","进程ID"));
        itemNameMap.put("Updated",new ItemName("Last updated","最后更新"));
        itemNameMap.put("COSSS",new ItemName("COS Server Status","COS服务状态"));
        itemNameMap.put("Version",new ItemName("Version","版本号"));
        itemNameMap.put("InitializationeE",new ItemName("Initialization Error","初始化出错"));
    }

    private static final String ICON_PATH_DEFAULT = "html/www/img/sharder-icon-def-128x128.png";
    private static final String TRAY_ICON_PATH_DEFAULT = "html/www/img/tray-icon-def-64x64.png";
    private static final String TRAY_ICON_PATH_MAC = "html/www/img/tray-icon-mac-320x320.png";
    private String getTrayIconPath(){
        String os = System.getProperty("os.name");
        if(os == null || "".equals(os)) return TRAY_ICON_PATH_DEFAULT;

        if("Mac OS X".equals(os)) return TRAY_ICON_PATH_MAC;

        return TRAY_ICON_PATH_DEFAULT;
    }

    void createAndShowGUI() {
        if (!SystemTray.isSupported()) {
            Logger.logInfoMessage("SystemTray is not supported");
            return;
        }
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        final PopupMenu popup = new PopupMenu();
        imageIcon = new ImageIcon(ICON_PATH_DEFAULT, "cos icon");
        trayIcon = new TrayIcon(new ImageIcon(getTrayIconPath(), "tray icon").getImage());
        trayIcon.setImageAutoSize(true);
        tray = SystemTray.getSystemTray();

        MenuItem shutdown = new MenuItem(getItemDisplay("Shutdown"));
        openWalletInBrowser = new MenuItem(getItemDisplay("OpenWIB"));
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            openWalletInBrowser.setEnabled(false);
        }
        MenuItem showDesktopApplication = new MenuItem(getItemDisplay("ShowDA"));
        MenuItem refreshDesktopApplication = new MenuItem(getItemDisplay("RefreshW"));
        if (!Conch.isDesktopMode()) {
            showDesktopApplication.setEnabled(false);
            refreshDesktopApplication.setEnabled(false);
        }
        viewLog = new MenuItem(getItemDisplay("ViewLF"));
        if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN))  viewLog.setEnabled(false);
        MenuItem status = new MenuItem(getItemDisplay("Status"));

        popup.add(showDesktopApplication);
        popup.add(status);
        // popup.add(viewLog);
        popup.addSeparator();
        popup.add(openWalletInBrowser);
        popup.add(refreshDesktopApplication);
        popup.addSeparator();
        popup.add(shutdown);
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip(getItemDisplay("Initializing"));
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.logInfoMessage("TrayIcon could not be added", e);
            return;
        }

        trayIcon.addActionListener(e -> displayStatus());

        openWalletInBrowser.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(dataProvider.getWallet());
            } catch (IOException ex) {
                Logger.logInfoMessage("Cannot open client in browser", ex);
            }
        });

        showDesktopApplication.addActionListener(e -> {
            try {
                Class.forName("org.conch.desktop.DesktopApplication").getMethod("launch").invoke(null);
            } catch (ReflectiveOperationException exception) {
                Logger.logInfoMessage("org.conch.desktop.DesktopApplication failed to launch", exception);
            }
        });

        refreshDesktopApplication.addActionListener(e -> {
            try {
                Class.forName("org.conch.desktop.DesktopApplication").getMethod("refresh").invoke(null);
            } catch (ReflectiveOperationException exception) {
                Logger.logInfoMessage("org.conch.desktop.DesktopApplication failed to refresh", exception);
            }
        });

        viewLog.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(dataProvider.getLogFile());
            } catch (IOException ex) {
                Logger.logInfoMessage("Cannot view log", ex);
            }
        });

        status.addActionListener(e -> displayStatus());

        shutdown.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog (null,
                    getItemDisplay("ShutdownTips"),
                    getItemDisplay("Shutdown"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Logger.logInfoMessage("Shutdown requested by System Tray");
                System.exit(0); // Implicitly invokes shutdown using the shutdown hook
            }
        });

        ActionListener statusUpdater = evt -> {
            if (statusDialog == null || !statusDialog.isVisible()) {
                return;
            }
            displayStatus();
        };
        new Timer(DELAY, statusUpdater).start();
    }

    private void displayStatus() {
        Block lastBlock = Conch.getBlockchain().getLastBlock();
        Collection<Generator> allGenerators = Generator.getAllGenerators();

        StringBuilder generators = new StringBuilder();
        for (Generator generator : allGenerators) {
            generators.append(Account.rsAccount(generator.getAccountId())).append(' ');
        }
        Object optionPaneBackground = UIManager.get("OptionPane.background");
        UIManager.put("OptionPane.background", Color.WHITE);
        Object panelBackground = UIManager.get("Panel.background");
        UIManager.put("Panel.background", Color.WHITE);
        Object textFieldBackground = UIManager.get("TextField.background");
        UIManager.put("TextField.background", Color.WHITE);
        Container statusPanelParent = null;
        if (statusDialog != null && statusPanel != null) {
            statusPanelParent = statusPanel.getParent();
            statusPanelParent.remove(statusPanel);
        }
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        addLabelRow(statusPanel, getItemDisplay("Installation"));
        addDataRow(statusPanel, getItemDisplay("Application"), Conch.APPLICATION);
        addDataRow(statusPanel, getItemDisplay("Version"), Conch.getFullVersion());
        addDataRow(statusPanel, getItemDisplay("Network"), getNetwork());
//        addDataRow(statusPanel, getItemDisplay("WorkingO"), "" + Constants.isOffline);
//        addDataRow(statusPanel, getItemDisplay("Browser"), String.valueOf(API.getWelcomePageUri()));
        addDataRow(statusPanel, getItemDisplay("PeerP"), String.valueOf(Conch.getPeerPort()));
        addDataRow(statusPanel, getItemDisplay("ProgramF"), String.valueOf(Paths.get(".").toAbsolutePath().getParent()));
//        addDataRow(statusPanel, getItemDisplay("UserF"), String.valueOf(Paths.get(Conch.getUserHomeDir()).toAbsolutePath()));
//        addDataRow(statusPanel, getItemDisplay("DatabaseU"), Db.db == null ? getItemDisplay("Unavailable") : Db.db.getUrl());
        addEmptyRow(statusPanel);

        if (lastBlock != null) {
            addLabelRow(statusPanel, getItemDisplay("LastB"));
            addDataRow(statusPanel, getItemDisplay("Height"), String.valueOf(lastBlock.getHeight()));
            addDataRow(statusPanel, getItemDisplay("Timestamp"), String.valueOf(lastBlock.getTimestamp()));
            addDataRow(statusPanel, getItemDisplay("Time"), Convert.dateFromEpochTime(lastBlock.getTimestamp()));
            addDataRow(statusPanel, getItemDisplay("SecondsP"), String.valueOf(Conch.getEpochTime() - lastBlock.getTimestamp()));
            addDataRow(statusPanel, getItemDisplay("Forging"), String.valueOf(allGenerators.size() > 0));
            if (allGenerators.size() > 0) {
                addDataRow(statusPanel, getItemDisplay("ForgingA"), generators.toString());
            }
        }
        
        int minerCount = 0;
        try {
            minerCount = Generator.getAllGenerators().size();
        }catch(Exception e){}
        
        addEmptyRow(statusPanel);
        addLabelRow(statusPanel, getItemDisplay("Environment"));
        addDataRow(statusPanel, getItemDisplay("NumberOP"), String.valueOf(Peers.getAllPeers().size()));
        addDataRow(statusPanel, getItemDisplay("AvailableP"), String.valueOf(minerCount));
        addDataRow(statusPanel, getItemDisplay("MaxM"), humanReadableByteCount(Runtime.getRuntime().maxMemory()));
        addDataRow(statusPanel, getItemDisplay("TotalM"), humanReadableByteCount(Runtime.getRuntime().totalMemory()));
        addDataRow(statusPanel, getItemDisplay("FreeM"), humanReadableByteCount(Runtime.getRuntime().freeMemory()));
        addDataRow(statusPanel, getItemDisplay("ProcessI"), Conch.getProcessId());
        addEmptyRow(statusPanel);
        addDataRow(statusPanel, getItemDisplay("Updated"), dateFormat.format(new Date()));
        if (statusDialog == null || !statusDialog.isVisible()) {
            JOptionPane pane = new JOptionPane(statusPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, imageIcon);
            statusDialog = pane.createDialog(wrapper, getItemDisplay("COSSS"));
            statusDialog.setVisible(true);
            statusDialog.dispose();
        } else {
            if (statusPanelParent != null) {
                statusPanelParent.add(statusPanel);
                statusPanelParent.revalidate();
            }
            statusDialog.getContentPane().validate();
            statusDialog.getContentPane().repaint();
            EventQueue.invokeLater(statusDialog::toFront);
        }
        UIManager.put("OptionPane.background", optionPaneBackground);
        UIManager.put("Panel.background", panelBackground);
        UIManager.put("TextField.background", textFieldBackground);
    }

    private void addDataRow(JPanel parent, String text, String value) {
        JPanel rowPanel = new JPanel();
        if (!"".equals(value)) {
            rowPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        }
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        if (!"".equals(text) && !"".equals(value)) {
            text += ':';
        }
        JLabel textLabel = new JLabel(text);
        // textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD));
        rowPanel.add(textLabel);
        rowPanel.add(Box.createRigidArea(new Dimension(140 - textLabel.getPreferredSize().width, 0)));
        JTextField valueField = new JTextField(value);
        valueField.setEditable(false);
        valueField.setBorder(BorderFactory.createEmptyBorder());
        rowPanel.add(valueField);
        rowPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        parent.add(rowPanel);
        parent.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private void addLabelRow(JPanel parent, String text) {
        addDataRow(parent, text, "");
    }

    private void addEmptyRow(JPanel parent) {
        addLabelRow(parent, "");
    }

    void setToolTip(final SystemTrayDataProvider dataProvider) {
        SwingUtilities.invokeLater(() -> {
            trayIcon.setToolTip(dataProvider.getToolTip());
            openWalletInBrowser.setEnabled(dataProvider.getWallet() != null && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
            viewLog.setEnabled(dataProvider.getWallet() != null);
            DesktopSystemTray.this.dataProvider = dataProvider;
        });
    }

    void shutdown() {
        SwingUtilities.invokeLater(() -> tray.remove(trayIcon));
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "" + ("KMGTPE").charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    void alert(String message) {
        JOptionPane.showMessageDialog(null, message, getItemDisplay("InitializationeE"), JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("os.name"));
    }
}
