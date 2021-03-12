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

package org.conch.env;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RuntimeEnvironment {



    public static final String RUNTIME_MODE_ARG = "sharder.runtime.mode";
    public static final String DIRPROVIDER_ARG = "sharder.runtime.dirProvider";
    public static final String NETWORK_ARG = "sharder.runtime.network";
    public static final String GUIDE_ARG = "sharder.runtime.guide";
    public static final String LOCAL_DEBUG_ARG = "sharder.runtime.localDebug";

    private static final String osname = System.getProperty("os.name").toLowerCase();
    private static final boolean isHeadless;
    private static final boolean hasJavaFX;
    static {
        boolean b;
        try {
            // Load by reflection to prevent exception in case java.awt does not exist
            Class graphicsEnvironmentClass = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadlessMethod = graphicsEnvironmentClass.getMethod("isHeadless");
            b = (Boolean)isHeadlessMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            b = true;
        }
        isHeadless = b;
        try {
            Class.forName("javafx.application.Application");
            b = true;
        } catch (ClassNotFoundException e) {
            System.out.println("javafx not supported");
            b = false;
        }
        hasJavaFX = b;
    }

    private static boolean isWindowsRuntime() {
        return osname.startsWith("windows");
    }

    private static boolean isUnixRuntime() {
        return osname.contains("nux") || osname.contains("nix") || osname.contains("aix") || osname.contains("bsd") || osname.contains("sunos");
    }

    private static boolean isMacRuntime() {
        return osname.contains("mac");
    }

    private static boolean isWindowsService() {
        return "service".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG)) && isWindowsRuntime();
    }

    private static boolean isHeadless() {
        return isHeadless;
    }

    private static boolean isDesktopEnabled() {
        return "desktop".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG)) && !isHeadless();
    }

    public static boolean isDesktopApplicationEnabled() {
        return isDesktopEnabled() && hasJavaFX;
    }

    public static RuntimeMode getRuntimeMode() {
        System.out.println("isHeadless=" + isHeadless());
        if (isDesktopEnabled()) {
            return new DesktopMode();
        } else if (isWindowsService()) {
            return new WindowsServiceMode();
        } else {
            return new CommandLineMode();
        }
    }

    public static DirProvider getDirProvider() {
        String dirProvider = System.getProperty(DIRPROVIDER_ARG);
        if (dirProvider != null) {
            try {
                return (DirProvider)Class.forName(dirProvider).newInstance();
            } catch (ReflectiveOperationException e) {
                System.out.println("Failed to instantiate dirProvider " + dirProvider);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        if (isDesktopEnabled()) {
            if (isWindowsRuntime()) {
                return new WindowsUserDirProvider();
            }
            if (isUnixRuntime()) {
                return new UnixUserDirProvider();
            }
            if (isMacRuntime()) {
                return new MacUserDirProvider();
            }
        }
        return new DefaultDirProvider();
    }

}
