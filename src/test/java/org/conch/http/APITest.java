package org.conch.http;

import org.conch.consensus.poc.hardware.SystemInfo;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class APITest {

    private static Server server;

    @BeforeAll
    static void init() {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(80);
        connector.setHost("localhost");
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        servletHandler.addServletWithMapping(APIServlet.class, "/sharder");
        try {
            server.start();
            System.out.println(String.format("Server %s ...", server.getState()));
            System.out.println(String.format("Server running at http://%s:%s", connector.getHost(), connector.getPort()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (server.isRunning()) server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @AfterAll
    static void destroy() {
        try {
            if (server.isRunning()) server.stop();
            System.out.println(String.format("Server %s ...", server.getState()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void run() {
        System.out.println("jetty test");
        Long[] openServers = new Long[]{
                Peer.Service.MINER.getCode(),
                Peer.Service.STORAGE.getCode(),
        };
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setCore(8).setAverageMHz(800)
                .setBandWidth(10).setHadPublicIp(true)
                .setMemoryTotal(16).setOpenServices(openServers)
                .setHardDiskSize(5000).setTradePerformance(16);
        PocTxBody.PocNodeConf pocNodeConf = new PocTxBody.PocNodeConf("localhost", "80", systemInfo);
        System.out.println("tx type: " + pocNodeConf.getTransactionType().getType() + "tx subType: " + pocNodeConf.getTransactionType().getSubtype());
        System.out.println("name: " + pocNodeConf.getAppendixName());
    }
}
