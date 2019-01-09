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

import org.conch.Conch;
import org.conch.env.service.ConchService_ServiceManagement;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestGenerator {

    public static void main(String[] args) {
        ManifestGenerator manifestGenerator = new ManifestGenerator();
        manifestGenerator.generate("./resource/cos.manifest.mf", Conch.class.getCanonicalName(), "./lib");
        String serviceClassName = ConchService_ServiceManagement.class.getCanonicalName();
        serviceClassName = serviceClassName.substring(0, serviceClassName.length() - "_ServiceManagement".length());
        manifestGenerator.generate("./resource/cosservice.manifest.mf", serviceClassName, "./lib");
    }

    private void generate(String fileName, String className, String ... directories) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, className);
        StringBuilder classpath = new StringBuilder();

        for (String directory : directories) {
            DirListing dirListing = new DirListing();
            try {
                Files.walkFileTree(Paths.get(directory), EnumSet.noneOf(FileVisitOption.class), 2, dirListing);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            classpath.append(dirListing.getClasspath());
        }
        classpath.append("conf/");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());
        try {
            manifest.write(Files.newOutputStream(Paths.get(fileName), StandardOpenOption.CREATE));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class DirListing extends SimpleFileVisitor<Path> {

        private final StringBuilder classpath = new StringBuilder();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            Path dir = file.subpath(file.getNameCount() - 2, file.getNameCount() - 1);
            classpath.append(dir).append('/').append(file.getFileName()).append(' ');
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            return FileVisitResult.CONTINUE;
        }

        public StringBuilder getClasspath() {
            return classpath;
        }
    }
}
