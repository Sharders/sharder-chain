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

package org.conch.desktop;

import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.conch.Conch;
import org.conch.http.API;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The class itself and methods in this class are invoked from JavaScript therefore has to be public
 */
@SuppressWarnings("WeakerAccess")
public class JavaScriptBridge {

    DesktopApplication application;
    private Clipboard clipboard;

    public JavaScriptBridge(DesktopApplication application) {
        this.application = application;
    }

    public void log(String message) {
        Logger.logInfoMessage(message);
    }

    @SuppressWarnings("unused")
    public void openBrowser(String account) {
        final String url = API.getWelcomePageUri().toString() + "?account=" + account;
        Platform.runLater(() -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                Logger.logInfoMessage("Cannot open " + API.getWelcomePageUri().toString() + " error " + e.getMessage());
            }
        });
    }

    @SuppressWarnings("unused")
    public String readContactsFile() {
        String fileName = "contacts.json";
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(Conch.getUserHomeDir(), fileName));
        } catch (IOException e) {
            Logger.logInfoMessage("Cannot read file " + fileName + " error " + e.getMessage());
            JSONObject response = new JSONObject();
            response.put("error", "contacts_file_not_found");
            response.put("file", fileName);
            response.put("folder", Conch.getUserHomeDir());
            response.put("type", "1");
            return JSON.toJSONString(response);
        }
        try {
            return new String(bytes, "utf8");
        } catch (UnsupportedEncodingException e) {
            Logger.logInfoMessage("Cannot parse file " + fileName + " content error " + e.getMessage());
            JSONObject response = new JSONObject();
            response.put("error", "unsupported_encoding");
            response.put("type", "2");
            return JSON.toJSONString(response);
        }
    }

    public String getAdminPassword() {
        return API.adminPassword;
    }

    @SuppressWarnings("unused")
    public void popupHandlerURLChange(String newValue) {
        application.popupHandlerURLChange(newValue);
    }

    @SuppressWarnings("unused")
    public boolean copyText(String text) {
        if (clipboard == null) {
            clipboard = Clipboard.getSystemClipboard();
            if (clipboard == null) {
                return false;
            }
        }
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        return clipboard.setContent(content);
    }

}