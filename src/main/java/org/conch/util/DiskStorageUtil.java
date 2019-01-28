package org.conch.util;

import org.conch.db.Db;

import java.io.*;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/12
 */
public class DiskStorageUtil {
    private static final String LOCAL_STORAGE_FOLDER = Db.getDir() + File.separator + "localstorage";

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
            Logger.logErrorMessage("save file failed[" + fileName + "]" + e.toString());
        }
    }

    public static Object getObjFromFile(String fileName) {
        try {
            ObjectInputStream ois =
                    new ObjectInputStream(new FileInputStream(getLocalStoragePath(fileName)));
            Object object = ois.readObject();
            return object;
        } catch (Exception e) {
            Logger.logErrorMessage("failed to read file [" + fileName + "]" + e.toString());
            return null;
        }
    }
}
