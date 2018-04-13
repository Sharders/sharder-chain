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

package org.conch.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class DbClause {

    public enum Op {

        LT("<"), LTE("<="), GT(">"), GTE(">="), NE("<>");

        private final String operator;

        Op(String operator) {
            this.operator = operator;
        }

        public String operator() {
            return operator;
        }
    }

    private final String clause;

    protected DbClause(String clause) {
        this.clause = clause;
    }

    final String getClause() {
        return clause;
    }

    protected abstract int set(PreparedStatement pstmt, int index) throws SQLException;

    public DbClause and(final DbClause other) {
        return new DbClause(this.clause + " AND " + other.clause) {
            @Override
            protected int set(PreparedStatement pstmt, int index) throws SQLException {
                index = DbClause.this.set(pstmt, index);
                index = other.set(pstmt, index);
                return index;
            }
        };
    }

    public static final DbClause EMPTY_CLAUSE = new FixedClause(" TRUE ");

    public static final class FixedClause extends DbClause {

        public FixedClause(String clause) {
            super(clause);
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            return index;
        }

    }

    public static final class NullClause extends DbClause {

        public NullClause(String columnName) {
            super(" " + columnName + " IS NULL ");
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            return index;
        }

    }

    public static final class NotNullClause extends DbClause {

        public NotNullClause(String columnName) {
            super(" " + columnName + " IS NOT NULL ");
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            return index;
        }

    }

    public static final class StringClause extends DbClause {

        private final String value;

        public StringClause(String columnName, String value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setString(index, value);
            return index + 1;
        }

    }

    public static final class LikeClause extends DbClause {

        private final String prefix;

        public LikeClause(String columnName, String prefix) {
            super(" " + columnName + " LIKE ? ");
            this.prefix = prefix.replace("%", "\\%").replace("_", "\\_") + '%';
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setString(index, prefix);
            return index + 1;
        }
    }

    public static final class LongClause extends DbClause {

        private final long value;

        public LongClause(String columnName, long value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        public LongClause(String columnName, Op operator, long value) {
            super(" " + columnName + operator.operator() + "? ");
            this.value = value;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, value);
            return index + 1;
        }
    }

    public static final class IntClause extends DbClause {

        private final int value;

        public IntClause(String columnName, int value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        public IntClause(String columnName, Op operator, int value) {
            super(" " + columnName + operator.operator() + "? ");
            this.value = value;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setInt(index, value);
            return index + 1;
        }

    }

    public static final class ByteClause extends DbClause {

        private final byte value;

        public ByteClause(String columnName, byte value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        public ByteClause(String columnName, Op operator, byte value) {
            super(" " + columnName + operator.operator() + "? ");
            this.value = value;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setByte(index, value);
            return index + 1;
        }

    }

    public static final class BooleanClause extends DbClause {

        private final boolean value;

        public BooleanClause(String columnName, boolean value) {
            super(" " + columnName + " = ? ");
            this.value = value;
        }

        @Override
        protected int set(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setBoolean(index, value);
            return index + 1;
        }

    }

}
