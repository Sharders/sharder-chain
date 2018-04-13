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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * CountingOutputWriter extends Writer to count the number of characters written
 */
public class CountingOutputWriter extends FilterWriter {

    /** Character count */
    private long count = 0;

    /**
     * Create the CountingOutputWriter for the specified writer
     *
     * @param   writer              Output writer
     */
    public CountingOutputWriter(Writer writer) {
        super(writer);
    }

    /**
     * Write a single character
     *
     * @param   c                   Character to be written
     * @throws  IOException         I/O error occurred
     */
    @Override
    public void write(int c) throws IOException {
        super.write(c);
        count++;
    }

    /**
     * Write an array of characters
     *
     * @param   cbuf                Characters to be written
     * @throws  IOException         I/O error occurred
     */
    @Override
    public void write(char[] cbuf) throws IOException {
        super.write(cbuf);
        count += cbuf.length;
    }

    /**
     * Write an array of characters starting at the specified offset
     *
     * @param   cbuf                Characters to be written
     * @param   off                 Starting offset
     * @param   len                 Number of characters to write
     * @throws  IOException         I/O error occurred
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        super.write(cbuf, off, len);
        count += len;
    }

    /**
     * Write a string
     *
     * @param   s                   String to be written
     * @throws  IOException         I/O error occurred
     */
    @Override
    public void write(String s) throws IOException {
        super.write(s);
        count += s.length();
    }

    /**
     * Write a substring
     *
     * @param   s                   String to be written
     * @param   off                 Starting offset
     * @param   len                 Number of characters to write
     * @throws  IOException         I/O error occurred
     */
    @Override
    public void write(String s, int off, int len) throws IOException {
        super.write(s, off, len);
        count += len;
    }

    /**
     * Return the number of characters written
     *
     * @return                      Character count
     */
    public long getCount() {
        return count;
    }
}
