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

import org.conch.db.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class PeerDb {

    static class Entry {
        private final String address;
        private final long services;
        private final int lastUpdated;

        Entry(String address, long services, int lastUpdated) {
            this.address = address;
            this.services = services;
            this.lastUpdated = lastUpdated;
        }

        public String getAddress() {
            return address;
        }

        public long getServices() {
            return services;
        }

        public int getLastUpdated() {
            return lastUpdated;
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null && (obj instanceof Entry) && address.equals(((Entry) obj).address));
        }
    }

    static void truncatePeerTable() throws SQLException {
        Connection con = null;
        PreparedStatement pstmt;
        try {
            con = Db.db.getConnection();
            pstmt = con.prepareStatement("TRUNCATE TABLE peer");
            pstmt.executeUpdate();
        } finally {
            DbUtils.close(con);
        }
    }

    static List<Entry> loadPeers() {
        List<Entry> peers = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt;
        try {
            con = Db.db.getConnection();
            pstmt = con.prepareStatement("SELECT * FROM peer");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    peers.add(new Entry(rs.getString("address"), rs.getLong("services"), rs.getInt("last_updated")));
                }
            } catch (SQLException e) {
                truncatePeerTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
        return peers;
    }

    static void deletePeers(Collection<Entry> peers) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?")) {
            try {
                for (Entry peer : peers) {
                    pstmt.setString(1, peer.getAddress());
                    pstmt.executeUpdate();
                }
            } catch (SQLException throwables) {
                truncatePeerTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static volatile int updatePeersCount = 0;

    static void updatePeers(Collection<Entry> peers) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("MERGE INTO peer "
                     + "(address, services, last_updated) KEY(address) VALUES(?, ?, ?)")) {
            try {
                for (Entry peer : peers) {
                    pstmt.setString(1, peer.getAddress());
                    pstmt.setLong(2, peer.getServices());
                    pstmt.setInt(3, peer.getLastUpdated());
                    pstmt.executeUpdate();
                }
            } catch (SQLException throwables) {
                truncatePeerTable();
                if (updatePeersCount == 0) {
                    updatePeersCount++;
                    updatePeers(peers);
                }
            }
            updatePeersCount = 0;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static volatile int updatePeerCount = 0;

    static void updatePeer(PeerImpl peer) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("MERGE INTO peer "
                     + "(address, services, last_updated) KEY(address) VALUES(?, ?, ?)")) {
            pstmt.setString(1, peer.getAnnouncedAddress());
            pstmt.setLong(2, peer.getServices());
            pstmt.setInt(3, peer.getLastUpdated());
            try {
                pstmt.executeUpdate();
            } catch (SQLException e) {
                truncatePeerTable();
                if (updatePeerCount == 0) {
                    updatePeerCount++;
                    updatePeer(peer);
                }
            }
            updatePeerCount = 0;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
