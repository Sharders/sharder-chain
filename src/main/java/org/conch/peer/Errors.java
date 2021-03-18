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

package org.conch.peer;

public final class Errors {

    public final static String BLACKLISTED = "Your peer is blacklisted";
    public final static String BLACKLISTED_BY_THEM = "You've blacklisted by them";
    public final static String END_OF_FILE = "Unexpected token END OF FILE at position 0.";
    public final static String UNKNOWN_PEER = "Your peer address cannot be resolved";
    public final static String UNSUPPORTED_REQUEST_TYPE = "Unsupported request type!";
    public final static String UNSUPPORTED_PROTOCOL = "Unsupported protocol!";
    public final static String INVALID_ANNOUNCED_ADDRESS = "Invalid announced address";
    public final static String SEQUENCE_ERROR = "Peer request received before 'getInfo' request";
    public final static String MAX_INBOUND_CONNECTIONS = "Maximum number of inbound connections exceeded";
    public final static String TOO_MANY_BLOCKS_REQUESTED = "Too many blocks requested";
    public final static String DOWNLOADING = "Blockchain download in progress";
    public final static String LIGHT_CLIENT = "Peer is in light mode";
    public final static String UNAUTHORIZED = "Your peer is unauthorized";
    public final static String CONNECT_ERROR = "Error with connection";

    private Errors() {
    } // never
}
