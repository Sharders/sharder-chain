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

package org.conch.db;

import it.sauronsoftware.cron4j.Scheduler;
import org.conch.Conch;
import org.conch.chain.BlockchainProcessor;
import org.conch.tools.ArchiveDbTool;

public class DbBackup {

    public static void init() {
        // auto archive listener
        if(ArchiveDbTool.openAutoArchive()){
            // AFTER_BLOCK_APPLY event listener
            Conch.getBlockchainProcessor().addListener(block -> ArchiveDbTool.checkAndArchiveDB(block),
                    BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        }

        // local db backup task
        Boolean enable = Conch.getBooleanProperty("sharder.db.enableBackup");
        if (enable) {
            String cron = Conch.getStringProperty("sharder.db.backup.cron").trim();
            if (cron.isEmpty() || cron == null) {
                return;
            }
            // Prepares the listener.
            DbBackupSchedulerListener listener = new DbBackupSchedulerListener();
            // Prepares the task.
            DbBackupTask task = new DbBackupTask();
            // Creates the scheduler.
            Scheduler scheduler = new Scheduler();
            // Registers the listener.
            scheduler.addSchedulerListener(listener);
            // Schedules the task.
            scheduler.schedule(cron, task);
            // Starts the scheduler.
            scheduler.start();
        }
    }
}
