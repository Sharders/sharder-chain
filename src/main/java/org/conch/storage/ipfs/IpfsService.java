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

package org.conch.storage.ipfs;

import fr.rhaz.events.EventManager;
import fr.rhaz.events.EventRunnable;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import org.conch.storage.Ssid;
import org.conch.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IpfsService {
    static IPFS ipfs;
    static Map configs = null;
    public static void init() {
        Daemon daemon = new Daemon();
        EventManager eventman =  daemon.getEventManager();
        eventman.register(new MyEvent());
        try {
            daemon.binaries(); // Download, extract, load binaries
        } catch (IOException e) {
            e.printStackTrace();
        }

        daemon.start(); // Init and start the daemon
        daemon.attach(); // Attach the API
        ipfs = daemon.getIPFS(); // Get the API object
        try {
            if(ipfs != null) {
                configs = ipfs.config.show();
            }else{
                Logger.logWarningMessage("ipfs instance is null, maybe initial failed!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String store(byte[] data) {
        try {
            NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(data);
            MerkleNode addResult = ipfs.add(file).get(0);
            return addResult.hash.toBase58();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] retrieve(String cid) throws IOException {
        return ipfs.get(Multihash.fromBase58(cid));
    }

    public static String pin(String cid) throws IOException {
        List<Multihash> list = ipfs.pin.add(Multihash.fromBase58(cid));
        Multihash multihash;
        if (list != null && list.size()>0) {
            multihash = list.get(0);
            return Ssid.encode(Ssid.Type.IPFS, multihash.toBase58());
        } else {
            return null;
        }
    }

    public static boolean unpin(String cid) {
        try {
            ipfs.pin.rm(Multihash.fromBase58(cid));
            return true;
        } catch (IOException | RuntimeException e) {
            Logger.logWarningMessage(e.getMessage());
            return false;
        }
    }

    public static String myAddress() {
        try {
            ArrayList addresses = (ArrayList)ipfs.id().get("Addresses");
            if (null != addresses) {
                MultiAddress address = new MultiAddress(addresses.get(addresses.size() -1).toString());
                return address.getHost();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class MyEvent extends EventRunnable<DaemonEvent> {

        @Override
        public void execute(DaemonEvent daemonEvent) {
            Logger.logInfoMessage("IPFS Event -> " + daemonEvent.getType().name());
        }
    }

}
