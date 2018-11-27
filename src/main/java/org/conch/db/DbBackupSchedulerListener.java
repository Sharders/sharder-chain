package org.conch.db;

import it.sauronsoftware.cron4j.SchedulerListener;
import it.sauronsoftware.cron4j.TaskExecutor;
import org.conch.util.Logger;

public class DbBackupSchedulerListener implements SchedulerListener {

    public void taskLaunching(TaskExecutor executor) {
        Logger.logInfoMessage("Db backup task " + executor.getGuid() + " is launching...");
    }

    public void taskSucceeded(TaskExecutor executor) {
        Logger.logInfoMessage("Db backup task " + executor.getGuid() + " Succeeded in " + (System.currentTimeMillis()-executor.getStartTime()));
    }

    public void taskFailed(TaskExecutor executor, Throwable exception) {
        Logger.logErrorMessage("Db backup task " + executor.getGuid() + " failed due to an exception["+exception.getMessage()+"]");
        exception.printStackTrace();
    }
}
