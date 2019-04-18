package org.conch.util;

import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.db.Db;

import java.io.*;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public class DiskStorageUtil {
    private DiskStorageUtil(){}
    
    private static final String FOLDER_NAME = "localstorage";
    private static String LOCAL_STORAGE_FOLDER = "";
    private static String baseDir = Db.getDir();
    
    static {
        storageFolderExist();
    }
    
    public static void storageFolderExist(){
        System.out.println("storageFolderExist");
        // storage folder path is same level as db folder
        
        if (StringUtils.isNotEmpty(baseDir)) {
            // if storage folder under the application, use it firstly
            File dirFile = new File(baseDir);
            if (dirFile.exists()) {
                LOCAL_STORAGE_FOLDER = Paths.get(dirFile.getParentFile().getAbsolutePath()).resolve(FOLDER_NAME).toString();
            }else{
                // append user home as prefix
                String dbDirUnderUserHome = Paths.get(Conch.getUserHomeDir(),baseDir).toString();
                dirFile = new File(dbDirUnderUserHome);
                if(!dirFile.exists()) {
                    dirFile.mkdir();
                }
                LOCAL_STORAGE_FOLDER = dirFile.getParentFile().getAbsolutePath() + File.separator + FOLDER_NAME;
            }
        }
        //check or create local storage folder
        File storageFolder = new File(LOCAL_STORAGE_FOLDER);
        if(!storageFolder.exists()) storageFolder.mkdir();
        System.out.println("storageFolderExist done");
    }
    
    
    public static String getLocalStoragePath(String fileName) {
        return Paths.get(LOCAL_STORAGE_FOLDER).resolve(fileName).toString();
    }

    public static void saveObjToFile(Object o, String fileName) {
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream oos = null;
        try {
            fileOutputStream = new FileOutputStream(getLocalStoragePath(fileName));
            oos = new ObjectOutputStream(fileOutputStream);
            oos.writeObject(o);
        } catch (Exception e) {
            Logger.logErrorMessage("save file failed[" + fileName + "]",e);
        }finally {
            if(oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    Logger.logWarningMessage("ObjectOutputStream close failed",e);
                }
            }
        }
    }

    public static Object getObjFromFile(String fileName) {
        Object object = null;
        ObjectInputStream ois = null;
        boolean fileExist = false;
        try {
            File file = new File(getLocalStoragePath(fileName));
            ;
            if(file != null && (fileExist=file.exists())) {
                ois = new ObjectInputStream(new FileInputStream(file));
                object = ois.readObject();
            }
        } catch (Exception e) {
            Logger.logWarningMessage("failed to read file [" + fileName + "]" ,e);
        } finally {
            if(ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    Logger.logWarningMessage("ObjectInputStream close failed",e);
                }
            }

            if(fileExist && object == null) {
                File file = new File(getLocalStoragePath(fileName));
                file.deleteOnExit();
                Logger.logWarningMessage("delete local cached file [" + fileName + "]");
            }
        }
        return object;
    }
}
