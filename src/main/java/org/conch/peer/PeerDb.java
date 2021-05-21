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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.conch.account.Account;
import org.conch.chain.BlockchainImpl;
import org.conch.db.*;
import org.conch.util.Logger;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

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

    public static ForkBlock loadForkBlock(Connection con, ResultSet rs) {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            long previousBlockId = rs.getLong("previous_block_id");
            long generatorId = rs.getLong("generator_id");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long nextBlockId = rs.getLong("next_block_id");
            int height = rs.getInt("height");
            long id = rs.getLong("id");
            return new ForkBlock(version, timestamp, id, generatorId, cumulativeDifficulty, height);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static ForkBlock.ForkBlockLinkedAccount loadForkBlocklinkedAccount(Connection con, ResultSet rs) {
        try {
            long accountId = rs.getLong("account_id");
            long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            return new ForkBlock.ForkBlockLinkedAccount(blockId, accountId, height);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Map<String, HashSet<ForkBlock.ForkBlockLinkedAccount>> getAllForkBlockLinkedAccountMap() {
        Connection con = null;
        HashMap<String, HashSet<ForkBlock.ForkBlockLinkedAccount>> hashMap = Maps.newHashMap();
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM fork_block_linked_account ORDER BY ACCOUNT_ID");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ForkBlock.ForkBlockLinkedAccount linkedAccount = loadForkBlocklinkedAccount(null, rs);
                    String generator = Account.rsAccount(linkedAccount.getAccountId());
                    if (hashMap.get(generator) == null) {
                        HashSet<ForkBlock.ForkBlockLinkedAccount> hashSet = Sets.newHashSet();
                        hashSet.add(linkedAccount);
                        hashMap.put(generator, hashSet);
                    } else {
                        HashSet<ForkBlock.ForkBlockLinkedAccount> forkBlockLinkedAccounts = hashMap.get(generator);
                        forkBlockLinkedAccounts.add(linkedAccount);
                        hashMap.put(generator, forkBlockLinkedAccounts);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
            return hashMap;
        }
    }

    public static DbIterator<ForkBlock.ForkBlockLinkedAccount> getAllForkBlockLinkedAccounts() {
        Connection con;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM fork_block_linked_account ORDER BY ACCOUNT_ID");
            return new DbIterator<>(con, pstmt, PeerDb::loadForkBlocklinkedAccount);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<ForkBlock> getAllForkBlocks() {
        Connection con;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM fork_block ORDER BY db_id ASC");
            return new DbIterator<>(con, pstmt, PeerDb::loadForkBlock);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void saveForkBlocks(Set<ForkBlock> forkBlocks) {
        Connection con = null;
        boolean isInTx = Db.db.isInTransaction();
        try {
            con = Db.db.getConnection();
            for (ForkBlock forkBlock : forkBlocks) {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO fork_block (ID, VERSION, "
                        + "TIMESTAMP, CUMULATIVE_DIFFICULTY, HEIGHT, GENERATOR_ID) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    int i = 0;
                    pstmt.setLong(++i, forkBlock.getId());
                    pstmt.setInt(++i, forkBlock.getVersion());
                    pstmt.setInt(++i, forkBlock.getTimestamp());
                    pstmt.setBytes(++i, forkBlock.getCumulativeDifficulty().toByteArray());
                    pstmt.setInt(++i, forkBlock.getHeight());
                    pstmt.setLong(++i, forkBlock.getGeneratorId());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }


    public static void saveForkBlockLinkedAccounts(Set<ForkBlock.ForkBlockLinkedAccount> forkBlocks) {
        Connection con = null;
        boolean isInTx = Db.db.isInTransaction();
        try {
            con = Db.db.getConnection();
            for (ForkBlock.ForkBlockLinkedAccount forkBlock : forkBlocks) {
                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO fork_block_linked_account (ACCOUNT_ID, "
                        + "BLOCK_ID, HEIGHT) "
                        + "VALUES (?, ?, ?)")) {
                    int i = 0;
                    pstmt.setLong(++i, forkBlock.getAccountId());
                    pstmt.setLong(++i, forkBlock.getBlockId());
                    pstmt.setInt(++i, forkBlock.getHeight());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    public static void deleteForkBlocksAndLinkedFromHeight(int height) {

        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                deleteForkBlocksAndLinkedFromHeight(height);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Logger.logErrorMessage(e.toString(), e);
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return;
        }
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM fork_block WHERE height < ?");
            PreparedStatement pstmt2 = con.prepareStatement("DELETE FROM fork_block_linked_account WHERE height < ?");
            pstmt.setInt(1, height);
            pstmt.executeUpdate();
            pstmt2.setInt(1, height);
            pstmt2.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
        Logger.logDebugMessage("Deleting forkBlocks and forkBlockLinkedAccounts starting from height %s", height);

    }

    public static void deleteForkBlocksAndLinkedFromAccount(String generator) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM fork_block_linked_account WHERE account_id = ?");
            pstmt.setLong(1, Account.rsAccountToId(generator));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
        Logger.logDebugMessage("Deleting forkBlockLinkedAccounts from account = %s", generator);
    }
}
