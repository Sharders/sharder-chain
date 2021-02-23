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

package org.conch.account;

import org.conch.Conch;
import org.conch.account.Account.ControlType;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.*;
import org.conch.db.*;
import org.conch.db.*;
import org.conch.db.VersionedEntityDbTable;
import org.conch.tx.*;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.vote.PhasingPoll;
import org.conch.vote.VoteWeighting.VotingModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public final class AccountRestrictions {

    public static final class PhasingOnly {

        public static PhasingOnly get(long accountId) {
            return phasingControlTable.get(phasingControlDbKeyFactory.newKey(accountId));
        }

        public static int getCount() {
            return phasingControlTable.getCount();
        }

        public static DbIterator<PhasingOnly> getAll(int from, int to) {
            return phasingControlTable.getAll(from, to);
        }

        public static void set(Account senderAccount, Attachment.SetPhasingOnly attachment) {
            PhasingParams phasingParams = attachment.getPhasingParams();
            if (phasingParams.getVoteWeighting().getVotingModel() == VotingModel.NONE) {
                //no voting - remove the control
                senderAccount.removeControl(ControlType.PHASING_ONLY);
                PhasingOnly phasingOnly = get(senderAccount.getId());
                phasingControlTable.delete(phasingOnly);
            } else {
                senderAccount.addControl(ControlType.PHASING_ONLY);
                PhasingOnly phasingOnly = get(senderAccount.getId());
                if (phasingOnly == null) {
                    phasingOnly = new PhasingOnly(senderAccount.getId(), phasingParams, attachment.getMaxFees(),
                            attachment.getMinDuration(), attachment.getMaxDuration());
                } else {
                    phasingOnly.phasingParams = phasingParams;
                    phasingOnly.maxFees = attachment.getMaxFees();
                    phasingOnly.minDuration = attachment.getMinDuration();
                    phasingOnly.maxDuration = attachment.getMaxDuration();
                }
                phasingControlTable.insert(phasingOnly);
            }
        }

        private final DbKey dbKey;
        private final long accountId;
        private PhasingParams phasingParams;
        private long maxFees;
        private short minDuration;
        private short maxDuration;

        private PhasingOnly(long accountId, PhasingParams params, long maxFees, short minDuration, short maxDuration) {
            this.accountId = accountId;
            dbKey = phasingControlDbKeyFactory.newKey(this.accountId);
            phasingParams = params;
            this.maxFees = maxFees;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        private PhasingOnly(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.dbKey = dbKey;
            Long[] whitelist = DbUtils.getArray(rs, "whitelist", Long[].class);
            phasingParams = new PhasingParams(rs.getByte("voting_model"),
                    rs.getLong("holding_id"),
                    rs.getLong("quorum"),
                    rs.getLong("min_balance"),
                    rs.getByte("min_balance_model"),
                    whitelist == null ? Convert.EMPTY_LONG : Convert.toArray(whitelist));
            this.maxFees = rs.getLong("max_fees");
            this.minDuration = rs.getShort("min_duration");
            this.maxDuration = rs.getShort("max_duration");
        }

        public long getAccountId() {
            return accountId;
        }

        public PhasingParams getPhasingParams() {
            return phasingParams;
        }

        public long getMaxFees() {
            return maxFees;
        }

        public short getMinDuration() {
            return minDuration;
        }

        public short getMaxDuration() {
            return maxDuration;
        }

        private void checkTransaction(Transaction transaction, boolean validatingAtFinish) throws ConchException.AccountControlException {
            if (!validatingAtFinish && maxFees > 0 && Math.addExact(transaction.getFeeNQT(), PhasingPoll.getSenderPhasedTransactionFees(transaction.getSenderId())) > maxFees) {
                throw new ConchException.AccountControlException(String.format("Maximum total fees limit of %f balance exceeded", ((double)maxFees)/Constants.ONE_SS));
            }
            if (transaction.getType() == TransactionType.Messaging.PHASING_VOTE_CASTING) {
                return;
            }
            try {
                phasingParams.checkApprovable();
            } catch (ConchException.NotCurrentlyValidException e) {
                Logger.logDebugMessage("Account control no longer valid: " + e.getMessage());
                return;
            }
            Appendix.Phasing phasingAppendix = transaction.getPhasing();
            if (phasingAppendix == null) {
                throw new ConchException.AccountControlException("Non-phased transaction when phasing account control is enabled");
            }
            if (!phasingParams.equals(phasingAppendix.getParams())) {
                throw new ConchException.AccountControlException("Phasing parameters mismatch phasing account control. Expected: " +
                        phasingParams.toString() + " . Actual: " + phasingAppendix.getParams().toString());
            }
            if (!validatingAtFinish) {
                int duration = phasingAppendix.getFinishHeight() - Conch.getBlockchain().getHeight();
                if ((maxDuration > 0 && duration > maxDuration) || (minDuration > 0 && duration < minDuration)) {
                    throw new ConchException.AccountControlException("Invalid phasing duration " + duration);
                }
            }
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_control_phasing "
                    + "(account_id, whitelist, voting_model, quorum, min_balance, holding_id, min_balance_model, "
                    + "max_fees, min_duration, max_duration, height, latest) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                DbUtils.setArrayEmptyToNull(pstmt, ++i, Convert.toArray(phasingParams.getWhitelist()));
                pstmt.setByte(++i, phasingParams.getVoteWeighting().getVotingModel().getCode());
                DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getQuorum());
                DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getVoteWeighting().getMinBalance());
                DbUtils.setLongZeroToNull(pstmt, ++i, phasingParams.getVoteWeighting().getHoldingId());
                pstmt.setByte(++i, phasingParams.getVoteWeighting().getMinBalanceModel().getCode());
                pstmt.setLong(++i, this.maxFees);
                pstmt.setShort(++i, this.minDuration);
                pstmt.setShort(++i, this.maxDuration);
                pstmt.setInt(++i, Conch.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

    }

    private static final DbKey.LongKeyFactory<PhasingOnly> phasingControlDbKeyFactory = new DbKey.LongKeyFactory<PhasingOnly>("account_id") {
        @Override
        public DbKey newKey(PhasingOnly rule) {
            return rule.dbKey;
        }
    };

    private static final VersionedEntityDbTable<PhasingOnly> phasingControlTable = new VersionedEntityDbTable<PhasingOnly>("account_control_phasing", phasingControlDbKeyFactory) {

        @Override
        protected PhasingOnly load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PhasingOnly(rs, dbKey);
        }

        @Override
        protected void save(Connection con, PhasingOnly phasingOnly) throws SQLException {
            phasingOnly.save(con);
        }
    };

    public static void init() {
    }

    public static void checkTransaction(Transaction transaction, boolean validatingAtFinish) throws ConchException.NotCurrentlyValidException {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        if (senderAccount == null) {
            throw new ConchException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId()) + " does not exist yet");
        }
        if (senderAccount.getControls().contains(Account.ControlType.PHASING_ONLY)) {
            PhasingOnly phasingOnly = PhasingOnly.get(transaction.getSenderId());
            phasingOnly.checkTransaction(transaction, validatingAtFinish);
        }
    }

    public static boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        if (!senderAccount.getControls().contains(Account.ControlType.PHASING_ONLY)) {
            return false;
        }
        if (PhasingOnly.get(transaction.getSenderId()).getMaxFees() == 0) {
            return false;
        }
        return transaction.getType() != TransactionType.AccountControl.SET_PHASING_ONLY &&
                TransactionType.isDuplicate(TransactionType.AccountControl.SET_PHASING_ONLY, Long.toUnsignedString(senderAccount.getId()),
                        duplicates, true);
    }

}
