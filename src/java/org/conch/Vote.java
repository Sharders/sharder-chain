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
import org.conch.tx.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Vote {

    private static final DbKey.LongKeyFactory<Vote> voteDbKeyFactory = new DbKey.LongKeyFactory<Vote>("id") {
        @Override
        public DbKey newKey(Vote vote) {
            return vote.dbKey;
        }
    };

    private static final EntityDbTable<Vote> voteTable = new EntityDbTable<Vote>("vote", voteDbKeyFactory) {

        @Override
        protected Vote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Vote(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Vote vote) throws SQLException {
            vote.save(con);
        }

        @Override
        public void trim(int height) {
            super.trim(height);
            try (Connection con = Db.db.getConnection();
                 DbIterator<Poll> polls = Poll.getPollsFinishingAtOrBefore(height, 0, Integer.MAX_VALUE);
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM vote WHERE poll_id = ?")) {
                for (Poll poll : polls) {
                    pstmt.setLong(1, poll.getId());
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    };

    public static int getCount() {
        return voteTable.getCount();
    }

    public static Vote getVote(long id) {
        return voteTable.get(voteDbKeyFactory.newKey(id));
    }

    public static DbIterator<Vote> getVotes(long pollId, int from, int to) {
        return voteTable.getManyBy(new DbClause.LongClause("poll_id", pollId), from, to);
    }

    public static Vote getVote(long pollId, long voterId){
        DbClause clause = new DbClause.LongClause("poll_id", pollId).and(new DbClause.LongClause("voter_id", voterId));
        return voteTable.getBy(clause);
    }

    public static Vote addVote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
        Vote vote = new Vote(transaction, attachment);
        voteTable.insert(vote);
        return vote;
    }

    static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final long pollId;
    private final long voterId;
    private final byte[] voteBytes;

    private Vote(Transaction transaction, Attachment.MessagingVoteCasting attachment) {
        this.id = transaction.getId();
        this.dbKey = voteDbKeyFactory.newKey(this.id);
        this.pollId = attachment.getPollId();
        this.voterId = transaction.getSenderId();
        this.voteBytes = attachment.getPollVote();
    }

    private Vote(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.pollId = rs.getLong("poll_id");
        this.voterId = rs.getLong("voter_id");
        this.voteBytes = rs.getBytes("vote_bytes");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO vote (id, poll_id, voter_id, "
                + "vote_bytes, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.pollId);
            pstmt.setLong(++i, this.voterId);
            pstmt.setBytes(++i, this.voteBytes);
            pstmt.setInt(++i, Conch.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getPollId() {
        return pollId;
    }

    public long getVoterId() {
        return voterId;
    }

    public byte[] getVoteBytes() {
        return voteBytes;
    }

}
