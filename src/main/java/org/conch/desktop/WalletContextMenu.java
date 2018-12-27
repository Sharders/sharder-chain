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

import com.sun.javafx.scene.control.skin.ContextMenuContent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Iterator;

/**
 * Show only the standard cut/copy/paste context menu for edit fields and labels
 * Hide the link and window context menus
 * <p>
 * Inspired by http://stackoverflow.com/questions/27047447/customized-context-menu-on-javafx-webview-webengine
 * Hopefully, in Java 9 there will be a more standard way to implement this.
 */
class WalletContextMenu implements EventHandler<ContextMenuEvent> {

    @Override
    public void handle(ContextMenuEvent event) {
        @SuppressWarnings("deprecation")
        final Iterator<Window> windows = Window.impl_getWindows(); // May not work in Java 9
        while (windows.hasNext()) {
            // access the context menu window
            final Window window = windows.next();
            if (window instanceof ContextMenu) {
                if (window.getScene() != null && window.getScene().getRoot() != null) {
                    Parent root = window.getScene().getRoot();
                    if (root.getChildrenUnmodifiable().size() > 0) {
                        Node popup = root.getChildrenUnmodifiable().get(0);
                        if (popup.lookup(".context-menu") != null) {
                            Node bridge = popup.lookup(".context-menu");
                            ContextMenuContent cmc = (ContextMenuContent) ((Parent) bridge).getChildrenUnmodifiable().get(0);
                            VBox itemsContainer = cmc.getItemsContainer();
                            for (Node node : itemsContainer.getChildren()) {
                                ContextMenuContent.MenuItemContainer item = (ContextMenuContent.MenuItemContainer)node;
                                if (item.getItem().getText().equals("Copy")) {
                                    return;
                                }
                            }
                            event.consume();
                            window.hide();
                            return;
                        }
                    }
                }
                return;
            }
        }
    }
}