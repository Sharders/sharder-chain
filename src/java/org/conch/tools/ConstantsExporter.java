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

package org.conch.tools;

import org.conch.http.GetConstants;
import org.conch.util.JSON;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ConstantsExporter {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: ConstantsExporter <destination constants.js file>");
            System.exit(1);
        }

        Writer writer;
        try {
            writer = new FileWriter(new File(args[0]));
            writer.write("if (!NRS) {\n" +
                    "    var NRS = {};\n" +
                    "    NRS.constants = {};\n" +
                    "}\n\n");
            writer.write("NRS.constants.SERVER = ");
            JSON.writeJSONString(GetConstants.getConstants(), writer);
            writer.write("\n\n" +
                    "if (isNode) {\n" +
                    "    module.exports = NRS.constants.SERVER;\n" +
                    "}\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
