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

package org.conch.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public final class CompareTraceFiles {

    public static void main(String[] args) {
        String testFile = args.length > 0 ? args[0] : "sharder-trace.csv";
        String defaultFile = args.length > 1 ? args[1] : "sharder-trace-default.csv";
        try (BufferedReader defaultReader = new BufferedReader(new FileReader(defaultFile));
             BufferedReader testReader = new BufferedReader(new FileReader(testFile))) {
            System.out.println(defaultReader.readLine());
            testReader.readLine();
            String testLine = testReader.readLine();
            if (testLine == null) {
                System.out.println("Empty trace file, nothing to compare");
                return;
            }
            int height = parseHeight(testLine);
            String defaultLine;
            while ((defaultLine = defaultReader.readLine()) != null) {
                if (parseHeight(defaultLine) >= height) {
                    break;
                }
            }
            if (defaultLine == null) {
                System.out.println("End of default trace file, can't compare further");
                return;
            }
            int endHeight = height;
            assertEquals(defaultLine, testLine);
            while ((testLine = testReader.readLine()) != null) {
                defaultLine = defaultReader.readLine();
                if (defaultLine == null) {
                    System.out.println("End of default trace file, can't compare further");
                    return;
                }
                endHeight = parseHeight(testLine);
                assertEquals(defaultLine, testLine);
            }
            if ((defaultLine = defaultReader.readLine()) != null) {
                if (parseHeight(defaultLine) <= endHeight) {
                    System.out.println("default height: " + parseHeight(defaultLine) + " end height: " + endHeight);
                }
            }
            System.out.println("Comparison with default trace file done from height " + height + " to " + endHeight);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static int parseHeight(String line) {
        return Integer.parseInt(line.substring(1, line.indexOf('\t') - 1));
    }

    private static void assertEquals(String defaultLine, String testLine) {
        if (!defaultLine.equals(testLine)) {
            System.out.println("Lines don't match:");
            System.out.println("default:\n" + defaultLine);
            System.out.println("test:\n" + testLine);
        }
    }

}
