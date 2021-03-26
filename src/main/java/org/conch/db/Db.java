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

import org.conch.Conch;
import org.conch.common.Constants;

public final class Db {

    public static final String PREFIX = Constants.isTestnetOrDevnet() ? "sharder.testDb" : "sharder.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Conch.getIntProperty("sharder.dbCacheKB"))
            .dbUrl(Conch.getStringProperty(PREFIX + "Url"))
            .dbType(Conch.getStringProperty(PREFIX + "Type"))
            .dbDir(getDir())
            .dbParams(Conch.getStringProperty(PREFIX + "Params"))
            .dbUsername(Conch.getStringProperty(PREFIX + "Username"))
            .dbPassword(Conch.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Conch.getIntProperty("sharder.maxDbConnections"))
            .loginTimeout(Conch.getIntProperty("sharder.dbLoginTimeout"))
            .defaultLockTimeout(Conch.getIntProperty("sharder.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Conch.getIntProperty("sharder.dbMaxMemoryRows"))
    );

    public static void init() {
        db.init(new ConchDbVersion());
    }

    public static void shutdown() {
        db.shutdown("COMPACT");
    }

    private Db() {} // never

    public static String getDir() {
        return Conch.getStringProperty(PREFIX + "Dir");
    }

    public static String getName() {
        return Constants.isTestnetOrDevnet() ? "ss_test_db" : "ss_db";
    }

}
