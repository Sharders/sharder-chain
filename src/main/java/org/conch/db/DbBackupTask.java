package org.conch.db;

import com.google.common.collect.Lists;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import org.conch.Conch;
import org.conch.common.Constants;
import org.h2.tools.Shell;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DbBackupTask extends Task{
    private static String defaultPath = Conch.getStringProperty("sharder.db.backup.path");


    @Override
    public boolean canBePaused() {
        return true;
    }

    @Override
    public boolean canBeStopped() {
        return true;
    }

    @Override
    public boolean supportsStatusTracking() {
        return true;
    }

    @Override
    public boolean supportsCompletenessTracking() {
        return true;
    }

    @Override
    public void execute(TaskExecutionContext taskExecutionContext) throws RuntimeException {
        if (Constants.isTestnet()) {
            executeTestnet(null, null);
        } else {
            execute(null, null);
        }
//        execute(null, null);
    }

    public static String execute(String path, String fileName) {
        String pathStr;
        String fileNameStr;
        try {
            Conch.getBlockchain().updateLock();
            Conch.getBlockchain().readLock();
            pathStr = path==null?defaultPath:path;
            if(fileName == null) {
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmSS");
                String now = currentDateTime.format(dateTimeFormatter);
                int height = Conch.getBlockchain().getHeight();
                fileNameStr = "mw_db_backup_"+now+"_"+ height +".zip";
            }else {
                fileNameStr = fileName + (fileName.contains(".zip")?"":".zip");
            }
            File file = new File(pathStr + (pathStr.endsWith(File.separator) ? "" : File.separator) +  fileNameStr);
            String sql = "SCRIPT TO '" + pathStr + (pathStr.endsWith(File.separator) ? "" : File.separator) +  fileNameStr +"' COMPRESSION ZIP";
            Shell shell = new Shell();
            shell.runTool(Db.db.getConnection(), "-sql", sql);

            deleteOldBackupFiles(pathStr, Lists.newArrayList(fileNameStr));

            return file.getAbsolutePath();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            Conch.getBlockchain().updateUnlock();
            Conch.getBlockchain().readUnlock();
        }

    }

    public static String executeTestnet(String path, String fileName) {
        String pathStr;
        String fileNameStr;
        try {
            Conch.getBlockchain().updateLock();
            Conch.getBlockchain().readLock();
            pathStr = path==null?defaultPath:path;
            if(fileName == null) {
                int height = Conch.getBlockchain().getHeight();
                fileNameStr = "mw_test_db_" + height +".zip";
            }else {
                fileNameStr = fileName + (fileName.contains(".zip")?"":".zip");
            }
            File file = new File(pathStr + (pathStr.endsWith(File.separator) ? "" : File.separator) +  fileNameStr);
            String sql = "SCRIPT TO '" + pathStr + (pathStr.endsWith(File.separator) ? "" : File.separator) +  fileNameStr +"' COMPRESSION ZIP";
            Shell shell = new Shell();
            shell.runTool(Db.db.getConnection(), "-sql", sql);

            deleteOldBackupFiles(pathStr, Lists.newArrayList(fileNameStr));

            return file.getAbsolutePath();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            Conch.getBlockchain().updateUnlock();
            Conch.getBlockchain().readUnlock();
        }
    }

    private static boolean deleteOldBackupFiles(String path, List<String> ignoreList){
        File file = new File(path);

        if(!file.isDirectory()) {
            return false;
        }
        
        File[] oldFiles = file.listFiles();
        for(int i = 0 ; i < oldFiles.length; i++){
            if(ignoreList.contains(oldFiles[i].getName())) {
                continue;
            }
            oldFiles[i].delete();
        }

        return true;
    }
}
