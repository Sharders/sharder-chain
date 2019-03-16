package org.conch.util;

import org.apache.commons.io.FileUtils;
import org.conch.common.ConchException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author CloudSen
 */
public class FileUtilTest {

    private static final String ZIP_SUFFIX = ".zip";
    private static final String SAVE_PATH = "temp/cos-hub-";
    private static final String DYNAMIC_DOWNLOAD_URL = "https://oss.sharder.org/cos/client/release/cos-hub-";
    private static final String VERSION_URL = "https://oss.sharder.org/cos/client/release/cos-latest-version";
    private static final String LATEST_VERSION_DEV_URL = "https://oss.sharder.org/cos/client/dev/cos-latest-version";
    private static final String DOWNLOAD_URL_0_1_1 = "https://oss.sharder.org/cos/client/release/cos-hub-0.1.1.zip";
    private static final String DOWNLOAD_URL_0_1_0 = "https://oss.sharder.org/cos/client/release/cos-hub-0.1.0.zip";

    public static void fetchUpgradePackageViaVersion() throws IOException, ConchException.NotValidException {
        String version = getNewestHubVersion();
        String url = DYNAMIC_DOWNLOAD_URL + version + ZIP_SUFFIX;
        System.out.println("[ UPGRADE INFO ] Url: " + url);
        File archive = new File(SAVE_PATH + version + ZIP_SUFFIX);
        if (!archive.exists()) {
            System.out.println("[ UPGRADE CLIENT ] Get upgrade package: " + archive.getName());
            FileUtils.copyURLToFile(new URL(url), archive);
        }
    }

    public static void fetchUpgradePackage() throws IOException {
        File archive = new File("temp/cos-hub-0.1.1.zip");
        System.out.println("[ UPGRADE INFO ] Url: " + DOWNLOAD_URL_0_1_1);
        if (!archive.exists()) {
            System.out.println("[ DOWNLOAD ] Downloading upgrade package: " + archive.getName());
            FileUtils.copyURLToFile(new URL(DOWNLOAD_URL_0_1_1), archive);
        }
    }

    public static String getNewestHubVersion() throws IOException, ConchException.NotValidException {
        Map<String, String> header = new HashMap<>(16);
        header.put("contentType", "application/zip");
        RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(LATEST_VERSION_DEV_URL).get().request();
        String version = Optional.ofNullable(response.getContent())
                .orElseThrow(() -> new ConchException.NotValidException("latest version can not be null"));
        version = version.replaceAll("[\r|\n]", "");
        System.out.println("[ VERSION ] Latest version is: " + version);
        return version;
    }

    public static void main(String[] args) {
        try {
            //fetchUpgradePackageViaVersion();
            getNewestHubVersion();
            //fetchUpgradePackage();
        } catch (IOException | ConchException.NotValidException e) {
            e.printStackTrace();
        }
    }
}
