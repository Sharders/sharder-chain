package org.conch.db;

/**
 * @author ben
 */
public class DbTrimUtils {

    public static void trimTables(int height, String... tables){
        if(tables == null || tables.length <= 0){
            return;
        }

        for(int i = 0; i < tables.length; i++){
            DerivedDbTable._trim(tables[i], height, false);
        }
    }
}
