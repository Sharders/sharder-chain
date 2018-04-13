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

package org.conch;

import org.conch.db.DbClause;
import org.conch.db.DbIterator;
import org.conch.db.DbKey;
import org.conch.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingVote {

    private static final DbKey.LinkKeyFactory<PhasingVote> phasingVoteDbKeyFactory = new DbKey.LinkKeyFactory<PhasingVote>("transaction_id", "voter_id") {
        @Override
        public DbKey newKey(PhasingVote vote) {
            return vote.dbKey;
        }
    };

    private static final EntityDbTable<PhasingVote> phasingVoteTable = new EntityDbTable<PhasingVote>("phasing_vote", phasingVoteDbKeyFactory) {

        @Override
        protected PhasingVote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PhasingVote(rs, dbKey);
        }

        @Override
        protected void save(Connection con, PhasingVote vote) throws SQLException {
            vote.save(con);
        }

    };

    public static DbIterator<PhasingVote> getVotes(long phasedTransactionId, int from, int to) {
        return phasingVoteTable.getManyBy(new DbClause.LongClause("transaction_id", phasedTransactionId), from, to);
    }

    public static PhasingVote getVote(long phasedTransactionId, long voterId) {
        return phasingVoteTable.get(phasingVoteDbKeyFactory.newKey(phasedTransactionId, voterId));
    }

    public static long getVoteCount(long phasedTransactionId) {
        return phasingVoteTable.getCount(new DbClause.LongClause("transaction_id", phasedTransactionId));
    }

    static void addVote(Transaction transaction, Account voter, long phasedTransactionId) {
        PhasingVote phasingVote = phasingVoteTable.get(phasingVoteDbKeyFactory.newKey(phasedTransactionId, voter.getId()));
        if (phasingVote == null) {
            phasingVote = new PhasingVote(transaction, voter, phasedTransactionId);
            phasingVoteTable.insert(phasingVote);
        }
    }

    static void init() {
    }

    private final long phasedTransactionId;
    private final long voterId;
    private final DbKey dbKey;
    private long voteId;

    private PhasingVote(Transaction transaction, Account voter, long phasedTransactionId) {
        this.phasedTransactionId = phasedTransactionId;
        this.voterId = voter.getId();
        this.dbKey = phasingVoteDbKeyFactory.newKey(this.phasedTransactionId, this.voterId);
        this.voteId = transaction.getId();
    }

    private PhasingVote(ResultSet rs, DbKey dbKey) throws SQLException {
        this.phasedTransactionId = rs.getLong("transaction_id");
        this.voterId = rs.getLong("voter_id");
        this.dbKey = dbKey;
        this.voteId = rs.getLong("vote_id");
    }

    public long getPhasedTransactionId() {
        return phasedTransactionId;
    }

    public long getVoterId() {
        return voterId;
    }

    public long getVoteId() {
        return voteId;
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_vote (vote_id, transaction_id, "
                + "voter_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.voteId);
            pstmt.setLong(++i, this.phasedTransactionId);
            pstmt.setLong(++i, this.voterId);
            pstmt.setInt(++i, Conch.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

}
