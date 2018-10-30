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

package org.conch.storage.ipfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;

import fr.rhaz.events.EventManager;
import org.conch.Conch;
import org.conch.Constants;
import org.conch.storage.ipfs.DaemonEvent.DaemonEventType;
import io.ipfs.api.IPFS;
import org.conch.util.Convert;
import org.conch.util.Logger;

public class Daemon{
    private IPFS ipfs;
    private OS os;
    private boolean attached = false;
    private Thread thread = null;
    private File bin;
    private File swarmKey;
    private boolean useServerProfile = Convert.nullToEmpty(Conch.getStringProperty("sharder.myAddress")).trim().length()>0;
    private String swarmPort = Conch.getStringProperty("sharder.storage.ipfs.swarm.port");
    private String apiPort = Conch.getStringProperty("sharder.storage.ipfs.api.port");
    private String gatewayPort = Conch.getStringProperty("sharder.storage.ipfs.gateway.port");

    private String ipfsStorePathStr = Conch.getStringProperty("sharder.storage.ipfs.storepath", "storage/ipfs/.ipfs");
    private File ipfsStorePath = new File(ipfsStorePathStr);
    private File storepath = new File("storage/ipfs");
    private File binspath = new File("storage/ipfs/bins");

    private Boolean enableGc = Conch.getBooleanProperty("sharder.storage.ipfs.deamon.enable-gc");
    private EventManager eventman;
    private Consumer<Process> gobbler;
    private Consumer<String> printer;
    private File binpath = storepath;

    public static enum OS {
        WINDOWS,
        MAC,
        LINUX,
        FREEBSD;
    }

    public void setBinPath(File path) {
        this.binpath = path;
    }

    public File getBinPath() {
        return binpath;
    }

    public File getStorePath() {
        return storepath;
    }

    public File getIpfsStorePath() {
        return ipfsStorePath;
    }

    public boolean isAttached() {
        return attached;
    }

    public IPFS getIPFS() {
        return ipfs;
    }

    public Thread getThread() {
        return thread;
    }

    public File getBin() {
        return bin;
    }

    public Daemon(Consumer<String> printer) {
        this.printer = printer;

        getOS();
        if(os == null || (os.equals(OS.FREEBSD) && !is64bits())) {
            print("System not supported");
            System.exit(1);
        }

        eventman = new EventManager();
        gobbler = defaultGobbler();
    }

    public Daemon() {
        this(defaultPrinter());
    }

    public EventManager getEventManager() {
        return eventman;
    }

    public void stop() {
        if(thread != null && thread.isAlive()) thread.interrupt();
    }

    public void start() {
        if(thread != null && thread.isAlive()) thread.interrupt();
        thread = run(new Runnable() {

            Process init;
            Process daemon;
            Process config;

            public void stop() {
                if(daemon.isAlive()) {
                    daemon.destroy();
                    print("Daemon stopped");
                    eventman.call(new DaemonEvent(DaemonEventType.DAEMON_STOPPED));
                }
            }

            @Override
            public void run() {

                Runtime.getRuntime().addShutdownHook(
                        new Thread(() -> stop())
                );

                new File(getStorePath(), "repo.lock").delete();

                try {

                    Runtime.getRuntime().addShutdownHook(
                            new Thread(() -> stop())
                    );

                    new File(getStorePath(), "repo.lock").delete();

                    // if nodes with public IPv4 address (servers, VPSes, etc.), disables host and content discovery in local networks.
                    String profileParam = useServerProfile?"--profile=server":"";
                    init = process("init", "-e", profileParam);
                    gobble(init);
                    eventman.call(new DaemonEvent(DaemonEventType.INIT_STARTED));
                    init.waitFor();
                    eventman.call(new DaemonEvent(DaemonEventType.INIT_DONE));

                } catch(InterruptedException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                swarmKey = new File(getIpfsStorePath(), "swarm.key");
                if (!swarmKey.exists()) {
                    print("move swarm key file from " + getStorePath() + " to " + getIpfsStorePath() + " ...");
                    try {
                        FileUtils.copyFileToDirectory(new File(getStorePath(), "swarm.key"), swarmKey.getParentFile() , false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Config
                File configFile = new File(getIpfsStorePath(), "config");
                JsonNode configRootNode = null;
                ObjectMapper objMapper = new ObjectMapper();
                try {
                    configRootNode = objMapper.readValue(configFile, ObjectNode.class);
                    JsonNode bootstrapNode = configRootNode.path("Bootstrap");
                    if (!bootstrapNode.isNull()) {
                        List<String> defaultBootstrapNodes = Constants.isTestnet() || Constants.isDevnet() ? Conch.getStringListProperty("sharder.storage.ipfs.bootstrap.defaultTestnetNodes")
                                : Conch.getStringListProperty("sharder.storage.ipfs.bootstrap.defaultNodes");
                        ((ObjectNode) configRootNode).putPOJO("Bootstrap", defaultBootstrapNodes.toArray());
                    }
                    JsonNode addressNode = configRootNode.path("Addresses");
                    JsonNode apiNode = addressNode.path("API");
                    // 127.0.0.1 means can be accessed locally
                    // 0.0.0.0 means anyone can accessed
                    // API: call api; Gateway: get and view the file by ssid; Swarm : connect to other node
                    if (!apiNode.isNull()) {
                        ((ObjectNode) addressNode).put("API", "/ip4/127.0.0.1/tcp/" + apiPort);
                    }
                    JsonNode gatewayNode = addressNode.path("Gateway");
                    if (!gatewayNode.isNull()) {
                        ((ObjectNode) addressNode).put("Gateway", "/ip4/127.0.0.1/tcp/" + gatewayPort);
                    }
                    JsonNode swarmNode = addressNode.path("Swarm");

                    if (!swarmNode.isNull()) {
                        ((ObjectNode) addressNode).putPOJO("Swarm", new String[]{
                                "/ip4/0.0.0.0/tcp/" + swarmPort,
                                "/ip6/::/tcp/" + swarmPort
                        });
                    }

                    objMapper.writeValue(configFile, configRootNode);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                try {
                    // enable-gc will auto delete unpin profile in storage network
                    daemon = process("daemon",enableGc?"--enable-gc":"","--enable-pubsub-experiment");
                    gobble(daemon);
                    eventman.call(new DaemonEvent(DaemonEventType.DAEMON_STARTED));
                    daemon.waitFor();

                } catch(InterruptedException e) {

                    stop();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getOS() {
        if(SystemUtils.IS_OS_WINDOWS)
            os = OS.WINDOWS;
        if(SystemUtils.IS_OS_LINUX)
            os = OS.LINUX;
        if(SystemUtils.IS_OS_MAC)
            os = OS.MAC;
        if(SystemUtils.IS_OS_FREE_BSD)
            os = OS.FREEBSD;
    }

    public boolean is64bits() {
        return System.getProperty("os.arch").contains("64");
    }

    public boolean isArm() {
        String arch = SystemUtils.OS_ARCH;
        return arch.contains("arm") || arch.contains("aarch");
    }

    public void binaries() throws IOException  {
        switch(os) {
            case WINDOWS:{
                bin = new File(binpath, "bin.exe");
                break;
            }
            case MAC: case LINUX: case FREEBSD: {
                bin = new File(binpath, "bin");
                break;
            }
        }
        if(!bin.exists()) getClient();
        if(!bin.canExecute()) {
            bin.setExecutable(true);
        }
    }

    public void getFileFromTarGz(String path, File arch, File destination) throws IOException{
        GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(arch));
        try(TarArchiveInputStream tis = new TarArchiveInputStream(gzis)){
            ArchiveEntry te;
            while((te = tis.getNextEntry()) != null) {
                if(!te.getName().equals(path)) continue;
                FileUtils.copyInputStreamToFile(tis, destination); break;
            }
        }
    }

    public void getFileFromZip(String path, File zip, File destination) throws IOException{
        try(ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))){
            ZipEntry ze;
            while((ze = zis.getNextEntry()) != null) {
                if(!ze.getName().equals(path)) continue;
                FileUtils.copyInputStreamToFile(zis, destination); break;
            }
        }
    }

    public void getClient() throws IOException{
        File archive;
        String path;
        String fileName;
        switch(os) {
            case WINDOWS:{
                fileName = "windows" + "-" + "amd64" + ".zip";
                break;
            }
            case MAC:{
                fileName = "darwin" + "-" + (is64bits()?"amd64":"386") + ".tar.gz";
                break;
            }
            case LINUX:{
                fileName = "linux" + "-" + (isArm()?"arm":(is64bits()?"amd64":"386")) + ".tar.gz";
                break;
            }
            case FREEBSD:{
                fileName = "freebsd" + "-"  + (isArm()?"arm":(is64bits()?"amd64":"386")) + ".tar.gz";
                break;
            }
            default: return;
        }

        switch(os) {
            case WINDOWS:{
                archive = new File(binpath, "bin.zip");
                path = "go-ipfs/ipfs.exe";
                break;
            }
            case MAC: case LINUX: case FREEBSD:{
                archive = new File(binpath, "bin.zip");
                path = "go-ipfs/ipfs";
                break;
            }
            default: return;
        }

        print("Copying bin from " + fileName + " ...");
        FileUtils.copyFile(new File(binspath, fileName), archive);
        if (fileName.contains("tar.gz")) {
            getFileFromTarGz(path, archive, bin);
        } else {
            getFileFromZip(path, archive, bin);
        }
        print("Bin successfully extracted");
        archive.delete();
    }

    public Thread run(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        return t;
    }

    public Thread run(boolean gobble, String... args) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process p = process(args);
                    if(gobble) gobble(p);
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        return t;
    }

    public void print(String msg) {
        printer.accept(msg);
    }

    public void setPrinter(Consumer<String> printer) {
        this.printer = printer;
    }

    public static Consumer<String> defaultPrinter(){
        return new Consumer<String>() {
            @Override
            public void accept(String msg) {
                Logger.logInfoMessage(msg);
            }
        };
    }

    public void gobble(Process p) {
        gobbler.accept(p);
    }

    public void setGobbler(Consumer<Process> gobbler) {
        this.gobbler = gobbler;
    }

    public static Consumer<Process> defaultGobbler(){
        return new Consumer<Process>() {
            @Override
            public void accept(Process p) {
                StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
                StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
                errorGobbler.start();
                outputGobbler.start();
            }

        };
    }

    public Process process(String... args) throws IOException{
        String[] cmd = ArrayUtils.insert(0, args, getBin().getPath());
        return Runtime.getRuntime().exec(cmd, new String[] {"IPFS_PATH="+ipfsStorePath.getAbsolutePath()});
    }

    public void attach() {
        this.attached = false;
        while(!attached) {
            try {
                ipfs = new IPFS("/ip4/127.0.0.1/tcp/" + apiPort);
//                ipfs.refs.local();
                attached  = true;
            } catch (Exception e) {}
        }
        eventman.call(new DaemonEvent(DaemonEventType.ATTACHED));
        print("Successfully attached");
    }

    /**
     * If you can't inst the Daemon in the Mac OS, you can call this method manually. We'll fix this environment problem later. xy-2018.10.18
     *
     * This method will init the ipfs server and generate the related files into specified repo
     */
    public static void initIPFS4Mac(){

        File ipfsRepo = new File(Conch.getStringProperty("sharder.storage.ipfs.storepath", "storage/ipfs/.ipfs"));
        File ipfsCmd = new File("storage/ipfs/bin");

        try {
            Runtime.getRuntime().exec(new String[]{ipfsCmd.getPath(),"init","-e"}, new String[] {"IPFS_PATH=" + ipfsRepo.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Manually init IPFS success.");
    }

    public static void main(String[] args) {
        initIPFS4Mac();
    }
}
