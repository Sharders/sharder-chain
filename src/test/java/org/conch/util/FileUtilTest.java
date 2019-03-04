package org.conch.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CloudSen
 */
public class FileUtilTest {

    private static final String VERSION_URL = "https://oss.sharder.org/cos/client/release/lastest-version";
    private static final String DOWNLOAD_URL = "https://oss.sharder.org/cos/client/release/cos-hub-0.1.1.zip";

    public static void fetchUpgradePackage() throws IOException {
        File archive = new File("temp/cos-hub-0.1.1.zip");
        if (!archive.exists()) {
            System.out.println("[UPGRADE CLIENT] Get upgrade package:" + archive.getName());
            FileUtils.copyURLToFile(new URL(DOWNLOAD_URL), archive);
        }
    }

    public static String getNewestHubVersion() throws IOException {
        Map<String, String> header = new HashMap<>(16);
        header.put("contentType", "application/zip");
        RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(VERSION_URL).get().request();
        String version = response.getContent();
        System.out.println("newest version is: " + version);
        return version;
    }

    public static void main(String[] args) {
        try {
            getNewestHubVersion();
            fetchUpgradePackage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
