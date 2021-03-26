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

package org.conch.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * MemoryHandler maintains a ring buffer of log messages.  The GetLog API is used
 * to retrieve these log messages.
 *
 * The following logging.properties entries are used:
 * <ul>
 * <li>org.conch.util.MemoryHandler.level (default ALL)</li>
 * <li>org.conch.util.MemoryHandler.size (default 100, minimum 10)</li>
 * </ul>
 */
public class MemoryHandler extends Handler {

    /** Default ring buffer size */
    private static final int DEFAULT_SIZE = 100;

    /** Level OFF value */
    private static final int OFF_VALUE = Level.OFF.intValue();

    /** Ring buffer */
    private final LogRecord[] buffer;

    /** Buffer start */
    private int start = 0;

    /** Number of buffer entries */
    private int count = 0;

    /** Publish level */
    private Level level;

    /**
     * Create a MemoryHandler and configure it based on LogManager properties
     */
    public MemoryHandler() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        String value;
        //
        // Allocate the ring buffer
        //
        int bufferSize;
        try {
            value = manager.getProperty(cname+".size");
            if (value != null)
                bufferSize = Math.max(Integer.valueOf(value.trim()), 10);
            else
                bufferSize = DEFAULT_SIZE;
        } catch (NumberFormatException exc) {
            bufferSize = DEFAULT_SIZE;
        }
        buffer = new LogRecord[bufferSize];
        //
        // Get publish level
        //
        try {
            value = manager.getProperty(cname+".level");
            if (value != null) {
                level = Level.parse(value.trim());
            } else {
                level = Level.ALL;
            }
        } catch (IllegalArgumentException exc) {
            level = Level.ALL;
        }
    }

    /**
     * Store a LogRecord in the ring buffer
     *
     * @param   record              Description of the log event. A null record is
     *                              silently ignored and is not published
     */
    @Override
    public void publish(LogRecord record) {
        if (record != null && record.getLevel().intValue() >= level.intValue() && level.intValue() != OFF_VALUE) {
            synchronized(buffer) {
                int ix = (start+count)%buffer.length;
                buffer[ix] = record;
                if (count < buffer.length) {
                    count++;
                } else {
                    start++;
                    start %= buffer.length;
                }
            }
        }
    }

    /**
     * Return the log messages from the ring buffer
     *
     * @param   msgCount            Number of messages to return
     * @return                      List of log messages
     */
    public List<String> getMessages(int msgCount) {
        List<String> rtnList = new ArrayList<>(buffer.length);
        synchronized(buffer) {
            int rtnSize = Math.min(msgCount, count);
            int pos = (start + (count-rtnSize))%buffer.length;
            Formatter formatter = getFormatter();
            for (int i=0; i<rtnSize; i++) {
                rtnList.add(formatter.format(buffer[pos++]));
                if (pos == buffer.length)
                    pos = 0;
            }
        }
        return rtnList;
    }

    /**
     * Flush the ring buffer
     */
    @Override
    public void flush() {
        synchronized(buffer) {
            start = 0;
            count = 0;
        }
    }

    /**
     * Close the handler
     */
    @Override public void close() {
        level = Level.OFF;
    }
}
