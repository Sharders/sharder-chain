/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt;

import nxt.db.BasicDb;
import nxt.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "sharder.testDb" : "sharder.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("sharder.dbCacheKB"))
            .dbUrl(Nxt.getStringProperty(PREFIX + "Url"))
            .dbType(Nxt.getStringProperty(PREFIX + "Type"))
            .dbDir(Nxt.getStringProperty(PREFIX + "Dir"))
            .dbParams(Nxt.getStringProperty(PREFIX + "Params"))
            .dbUsername(Nxt.getStringProperty(PREFIX + "Username"))
            .dbPassword(Nxt.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Nxt.getIntProperty("sharder.maxDbConnections"))
            .loginTimeout(Nxt.getIntProperty("sharder.dbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("sharder.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Nxt.getIntProperty("sharder.dbMaxMemoryRows"))
    );

    static void init() {
        db.init(new ConchDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
