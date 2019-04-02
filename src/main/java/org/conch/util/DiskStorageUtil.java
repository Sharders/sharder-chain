package org.conch.util;

import org.apache.commons.lang3.StringUtils;
import org.conch.db.Db;

import java.io.*;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public class DiskStorageUtil {
    //private static final String LOCAL_STORAGE_FOLDER = Db.getDir() + File.separator + "localstorage";
    private static final String FOLDER_NAME = "localstorage";
    private static String LOCAL_STORAGE_FOLDER = "";
    private static boolean storageFolderExist = false;

    public static String getLocalStoragePath(String fileName) {
        if(storageFolderExist) {
            return LOCAL_STORAGE_FOLDER + File.separator + fileName;
        }
        
        String dbDir = Db.getDir();
        if (StringUtils.isNotEmpty(dbDir)) {
            File dirFile = new File(dbDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
                LOCAL_STORAGE_FOLDER = dirFile.getParentFile().getAbsolutePath() ;
                storageFolderExist = true;
            }
        }
        return LOCAL_STORAGE_FOLDER + File.separator + fileName;
    }

    public static void saveObjToFile(Object o, String fileName) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getLocalStoragePath(fileName)));
            oos.writeObject(o);
            oos.close();
        } catch (Exception e) {
            Logger.logErrorMessage("save file failed[" + fileName + "]",e);
        }
    }

    public static Object getObjFromFile(String fileName) {
        Object object = null;
        ObjectInputStream ois = null;
        try {
            File file = new File(getLocalStoragePath(fileName));
            if(file != null && file.exists()) {
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
            
            if(object == null) {
                File file = new File(getLocalStoragePath(fileName));
                file.deleteOnExit();
                Logger.logWarningMessage("delete local cached file [" + fileName + "]");
            }
        }
        return object;
    }
}
