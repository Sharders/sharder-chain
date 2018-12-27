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

final class Errors {

    final static String BLACKLISTED = "Your peer is blacklisted";
    final static String END_OF_FILE = "Unexpected token END OF FILE at position 0.";
    final static String UNKNOWN_PEER = "Your peer address cannot be resolved";
    final static String UNSUPPORTED_REQUEST_TYPE = "Unsupported request type!";
    final static String UNSUPPORTED_PROTOCOL = "Unsupported protocol!";
    final static String INVALID_ANNOUNCED_ADDRESS = "Invalid announced address";
    final static String SEQUENCE_ERROR = "Peer request received before 'getInfo' request";
    final static String MAX_INBOUND_CONNECTIONS = "Maximum number of inbound connections exceeded";
    final static String TOO_MANY_BLOCKS_REQUESTED = "Too many blocks requested";
    final static String DOWNLOADING = "Blockchain download in progress";
    final static String LIGHT_CLIENT = "Peer is in light mode";
    final static String UNAUTHORIZED = "Your peer is unauthorized";
    final static String CONNECTERROR = "Error with connection";

    private Errors() {} // never
}
