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

package org.conch.http;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class GetPlugins extends APIServlet.APIRequestHandler {

    static final GetPlugins instance = new GetPlugins();

    private GetPlugins() {
        super(new APITag[] {APITag.INFO});
    }

    private static final Path PLUGINS_HOME = Paths.get("./html/www/plugins");

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        if (!Files.isReadable(PLUGINS_HOME)) {
            return JSONResponses.fileNotFound(PLUGINS_HOME.toString());
        }
        PluginDirListing pluginDirListing = new PluginDirListing();
        try {
            Files.walkFileTree(PLUGINS_HOME, EnumSet.noneOf(FileVisitOption.class), 2, pluginDirListing);
        } catch (IOException e) {
            return JSONResponses.fileNotFound(e.getMessage());
        }
        JSONArray plugins = new JSONArray();
        pluginDirListing.getDirectories().forEach(dir -> plugins.add(Paths.get(dir.toString()).getFileName().toString()));
        response.put("plugins", plugins);
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    private static class PluginDirListing extends SimpleFileVisitor<Path> {

        private final List<Path> directories = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
            if (!PLUGINS_HOME.equals(dir)) {
                directories.add(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        public List<Path> getDirectories() {
            return directories;
        }
    }

}
