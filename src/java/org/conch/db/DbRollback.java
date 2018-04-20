package org.conch.db;
import org.conch.Conch;
import org.conch.Db;
import org.conch.util.Logger;
import org.h2.tools.Shell;
import java.sql.SQLException;

public class DbRollback{

    public static void rollback(String scriptFile) throws SQLException {
        try {
            Conch.getBlockchain().updateLock();
            Conch.getBlockchain().readLock();
            Logger.logInfoMessage("Db rollback task is launching...");
            String sql = "DROP ALL OBJECTS;RUNSCRIPT FROM '"+scriptFile+"' COMPRESSION ZIP";
            Shell shell = new Shell();
            shell.runTool(Db.db.getConnection(), "-sql", sql);
            Logger.logInfoMessage("Db rollback task done");
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Conch.getBlockchain().updateUnlock();
            Conch.getBlockchain().readUnlock();
            Conch.shutdown();
            Logger.logInfoMessage("Conch.init ...");
        }
    }

}
