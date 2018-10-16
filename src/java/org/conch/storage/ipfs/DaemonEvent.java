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

package org.conch.storage.ipfs;

import fr.rhaz.events.Event;

public class DaemonEvent extends Event {
    private DaemonEventType type;

    public DaemonEvent(DaemonEventType type) {
        this.type = type;
    }

    public DaemonEventType getType() {
        return type;
    }

    public static enum DaemonEventType{
        INIT_STARTED,
        INIT_DONE,
        CONFIG_STARTED,
        CONFIG_DONE,
        DAEMON_STARTED,
        DAEMON_STOPPED,
        ATTACHED;
    }
}
