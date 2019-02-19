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
    private static final String LOCAL_STORAGE_FOLDER = initPath();
    private static String initPath() {
        String pathPrefix = "tmp";
        String dbDir = Db.getDir();
        if (StringUtils.isNotEmpty(dbDir)) {
            File dirFile = new File(dbDir);
            if (dirFile.exists()) {
                pathPrefix = dirFile.getParentFile().getAbsolutePath() ;
            }
        }
        return pathPrefix + File.separator + FOLDER_NAME;
    }

    public static String getLocalStoragePath(String fileName) {
        return LOCAL_STORAGE_FOLDER + File.separator + fileName;
    }

    public static void saveObjToFile(Object o, String fileName) {
        try {
            File localStorageFolder = new File(LOCAL_STORAGE_FOLDER);
            if (!localStorageFolder.exists()) localStorageFolder.mkdir();

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getLocalStoragePath(fileName)));
            oos.writeObject(o);
            oos.close();
        } catch (Exception e) {
            Logger.logErrorMessage("save file failed[" + fileName + "]",e);
        }
    }

    public static Object getObjFromFile(String fileName) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getLocalStoragePath(fileName)));
            Object object = ois.readObject();
            return object;
        } catch (Exception e) {
            Logger.logErrorMessage("failed to read file [" + fileName + "]" ,e);
            return null;
        }
    }
}
