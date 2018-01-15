/*
 * Copyright © 2017 sharder.org.
 * Copyright © 2014-2017 ichaoj.com.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,
 * no part of the COS software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.conch;

import org.conch.db.BasicDb;
import org.conch.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "sharder.testDb" : "sharder.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Conch.getIntProperty("sharder.dbCacheKB"))
            .dbUrl(Conch.getStringProperty(PREFIX + "Url"))
            .dbType(Conch.getStringProperty(PREFIX + "Type"))
            .dbDir(Conch.getStringProperty(PREFIX + "Dir"))
            .dbParams(Conch.getStringProperty(PREFIX + "Params"))
            .dbUsername(Conch.getStringProperty(PREFIX + "Username"))
            .dbPassword(Conch.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Conch.getIntProperty("sharder.maxDbConnections"))
            .loginTimeout(Conch.getIntProperty("sharder.dbLoginTimeout"))
            .defaultLockTimeout(Conch.getIntProperty("sharder.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Conch.getIntProperty("sharder.dbMaxMemoryRows"))
    );

    static void init() {
        db.init(new ConchDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
