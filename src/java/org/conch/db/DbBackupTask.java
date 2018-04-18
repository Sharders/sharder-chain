package org.conch.db;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import org.conch.Conch;
import org.conch.Db;
import org.h2.tools.Shell;
import org.h2.util.LocalDateTimeUtils;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class DbBackupTask extends Task{
    String path = Conch.getStringProperty("sharder.db.backup.path");
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
        try {
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmSS");
            String now = currentDateTime.format(dateTimeFormatter);
            String sql = "BACKUP TO '" + path + File.separator + "sharder_db_backup_"+now+".zip'";
            Shell shell = new Shell();
            shell.runTool(Db.db.getConnection(), "-sql", sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
