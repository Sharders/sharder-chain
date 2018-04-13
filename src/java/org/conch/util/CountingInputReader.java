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

import org.conch.ConchException;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * CountingInputReader extends Reader to count the number of characters read
 */
public class CountingInputReader extends FilterReader {

    /** Current character count */
    private long count = 0;

    /** Maximum character count */
    private final long limit;

    /**
     * Create a CountingInputReader for the supplied Reader
     *
     * @param   reader              Input reader
     * @param   limit               Maximum number of characters to be read
     */
    public CountingInputReader(Reader reader, long limit) {
        super(reader);
        this.limit = limit;
    }

    /**
     * Read a single character
     *
     * @return                      Character or -1 if end of stream reached
     * @throws  IOException         I/O error occurred
     */
    @Override
    public int read() throws IOException {
        int c = super.read();
        if (c != -1)
            incCount(1);
        return c;
    }

    /**
     * Read characters into an array
     *
     * @param   cbuf                Character array
     * @return                      Number of characters read or -1 if end of stream reached
     * @throws  IOException         I/O error occurred
     */
    @Override
    public int read(char [] cbuf) throws IOException {
        int c = super.read(cbuf);
        if (c != -1)
            incCount(c);
        return c;
    }

    /**
     * Read characters into an arry starting at the specified offset
     *
     * @param   cbuf                Character array
     * @param   off                 Starting offset
     * @param   len                 Number of characters to be read
     * @return                      Number of characters read or -1 if end of stream reached
     * @throws  IOException         I/O error occurred
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int c = super.read(cbuf, off, len);
        if (c != -1)
            incCount(c);
        return c;
    }

    /**
     * Skip characters in the input stream
     *
     * @param   n                   Number of characters to skip
     * @return                      Number of characters skipped or -1 if end of stream reached
     * @throws  IOException         I/O error occurred
     */
    @Override
    public long skip(long n) throws IOException {
        long c = super.skip(n);
        if (c != -1)
            incCount(c);
        return c;
    }

    /**
     * Return the total number of characters read
     *
     * @return                      Character count
     */
    public long getCount() {
        return count;
    }

    /**
     * Increment the character count and check if the maximum count has been exceeded
     *
     * @param   c                   Number of characters read
     * @throws ConchException.ConchIOException      Maximum count exceeded
     */
    private void incCount(long c) throws ConchException.ConchIOException {
        count += c;
        if (count > limit)
            throw new ConchException.ConchIOException("Maximum size exceeded: " + count);
    }
}
