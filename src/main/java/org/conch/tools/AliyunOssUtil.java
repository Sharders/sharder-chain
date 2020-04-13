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

package org.conch.tools;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.mint.Generator;
import java.io.*;
import java.util.*;


public class AliyunOssUtil {

    // OSS setting
    private static final String ENDPOINT = "http://" + Conch.getStringProperty("sharder.oss.url");
    private static final String INTERNAL_ENDPOINT = "http://" + Conch.getStringProperty("sharder.oss.internal-url");
    private static final String ACCESS_KEY_ID = Conch.getStringProperty("sharder.oss.key");
    private static final String ACCESS_KEY_SECRET = Conch.getStringProperty("sharder.oss.secret");
    private static final String BUCKET_NAME = Conch.getStringProperty("sharder.oss.bucket");

    /**
     * auto db back
     * @return
     */
    protected static boolean openAutoBackDB(){
        if(StringUtils.isEmpty(ACCESS_KEY_ID)
            || StringUtils.isEmpty(ACCESS_KEY_SECRET)
            || StringUtils.isEmpty(BUCKET_NAME) ) return false;

        return true;
    }


    /**
     * upload file to OSS
     * @param ossStorePath store path on the OSS
     * @param filePath local file path
     * @param delAfter true: delete the local file after upload success
     * @throws IOException
     */
    public static void uploadFile(String ossStorePath, String filePath, boolean delAfter) {
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        try {
            InputStream inputStream = new FileInputStream(filePath);
            try {
                ossClient.putObject(BUCKET_NAME, ossStorePath, inputStream);
                if(delAfter) {
                    new File(filePath).delete();
                }
            } catch (Exception e) {
                System.err.printf("May be your path is internal path, try the public path",e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.err.printf("File is missing on the path: " + filePath + ". CAUSE BY: " + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }


    /**
     * download the file form OSS
     * @param objPath object path on the OSS
     * @throws IOException
     */
    public static void downloadFile(String objPath) throws IOException {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);

        // 调用ossClient.getObject返回一个OSSObject实例，该实例包含文件内容及文件元信息。
        OSSObject ossObject = ossClient.getObject(BUCKET_NAME, objPath);
        // 调用ossObject.getObjectContent获取文件输入流，可读取此输入流获取其内容。
        InputStream content = ossObject.getObjectContent();
        if (content != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("\n" + line);
            }
            // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            content.close();
        }

        // 关闭OSSClient。
        ossClient.shutdown();
    }

    /**
     * download the file form OSS
     * @param objPath object path on the OSS
     * @param savePath local save path
     */
    public static void downloadFileToLocal(String objPath, String savePath) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        // 下载OSS文件到本地文件。如果指定的本地文件存在会覆盖，不存在则新建。
        File file = new File(savePath);
        if (!file.exists()){
            file.getParentFile().mkdirs();
        }
        ossClient.getObject(new GetObjectRequest(BUCKET_NAME, objPath), file);
        // 关闭OSSClient。
        ossClient.shutdown();
    }

    /**
     * delete the specified list
     * @param list deletion list
     * @return
     */
    public static boolean delFile(List<String> list) {
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        DeleteObjectsRequest dt = new DeleteObjectsRequest(BUCKET_NAME).withQuiet(true).withKeys(list);

        DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(dt);
        List<String> delFails = null;
        try {
            delFails = deleteObjectsResult.getDeletedObjects();
        } catch (Exception e) {
            System.err.printf("May be your path is internal path, try the public path",e.getMessage());
        }

        if(delFails == null || delFails.size() == 0){
            System.out.println("delete operation is success");
        }else{
            System.out.println("part of the list deletion failed: " +  delFails.toString());
        }

        ossClient.shutdown();
        return delFails.size() < 1;
    }

    /**
     * @author SOFAS
     * @description         获取指定文件夹下面的所有文件
     * @date  2019/9/9
     * @param path          指定文件夹
     * @return java.util.List<com.aliyun.oss.model.OSSObjectSummary>
     **/
    public static void getFile(String path, List<String> files){
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(BUCKET_NAME);
        // 设置prefix参数来获取fun目录下的所有文件。
        listObjectsRequest.setPrefix(path);
        // 递归列出fun目录下的所有文件。
        ObjectListing listing = ossClient.listObjects(listObjectsRequest);
        // 关闭OSSClient。
        ossClient.shutdown();
        // 遍历所有文件。
        for (OSSObjectSummary objectSummary : listing.getObjectSummaries()) {
            files.add(objectSummary.getKey());
        }
        // 遍历所有commonPrefix。
        for (String commonPrefix : listing.getCommonPrefixes()) {
            getFile(commonPrefix, files);
        }
    }

    public static void delByPath(String path) {
        List<String> files = new ArrayList<>();
        if (path != null){
            AliyunOssUtil.getFile(path,files);
        }else {
           return;
        }
        if (files != null && files.size() > 0) {
            AliyunOssUtil.delFile(files);
        }
    }

    /**
     *
     * @param filepath
     * @param map
     * @return
     */
    public static String readAllFile(String filepath, HashMap<String, String> map) {
        File file= new File(filepath);
        if(file.isDirectory()){
            String[] list = file.list();
            if (list != null && list.length > 0){
                for (String s : list) {
                    File readFile = new File(filepath);
                    if (readFile.isDirectory()) {
                        readAllFile(filepath + "/" + s, map);
                    } else {
                        map.put(filepath, filepath);
                    }
                }
            }
        }else {
            map.put(filepath, filepath);
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        String s = "30.zip";
        String s1 = "D:\\aa\\" + s;
        String s2 = "archive/online/upload/uploadfile/pdfUpload/2019-08/" + s;
        downloadFileToLocal(s2, s1);
        /*解密并输出压缩文件*/
        //SxqFileUtils.byteToFile(AESCoder.decrypt(SxqFileUtils.fileToByte(s1),"7b03d498836943d3ab0fac2abcf29365"), s1);
        /*把压缩文件进行解压*/
/*        SxqFileUtils.unZipFiles(s1, s1.substring(0, s1.length() - 7));*/
    }
}
