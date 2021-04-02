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

package org.conch.vote;

import java.sql.ResultSet;
import java.sql.SQLException;

abstract class AbstractPoll {

    final long id;
    final VoteWeighting voteWeighting;
    final long accountId;
    final int finishHeight;

    AbstractPoll(long id, long accountId, int finishHeight, VoteWeighting voteWeighting) {
        this.id = id;
        this.accountId = accountId;
        this.finishHeight = finishHeight;
        this.voteWeighting = voteWeighting;
    }

    AbstractPoll(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.finishHeight = rs.getInt("finish_height");
        this.voteWeighting = new VoteWeighting(rs.getByte("voting_model"), rs.getLong("holding_id"),
                rs.getLong("min_balance"), rs.getByte("min_balance_model"));
    }

    public final long getId() {
        return id;
    }

    public final long getAccountId() {
        return accountId;
    }

    public final int getFinishHeight() {
        return finishHeight;
    }

    public final VoteWeighting getVoteWeighting() {
        return voteWeighting;
    }

}

