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
import org.conch.asset.Asset;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.db.DerivedDbTable;
import org.conch.tx.TransactionDb;
import org.conch.util.JSON;
import org.conch.util.Listener;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

import static org.conch.db.Db.db;

public final class FxtDistribution implements Listener<Block> {

    public static final int DISTRIBUTION_END = Constants.FXT_BLOCK;
    public static final int DISTRIBUTION_START = DISTRIBUTION_END - 90 * 1440; // run for 90 days
    public static final int DISTRIBUTION_FREQUENCY = 720; // run processing every 720 blocks
    public static final int DISTRIBUTION_STEP = 60; // take snapshots every 60 blocks

    public static final long FXT_ASSET_ID = -1L;
    public static final long FXT_ISSUER_ID = -1L;

    private static final BigInteger BALANCE_DIVIDER = BigInteger.valueOf(10000L * (DISTRIBUTION_END - DISTRIBUTION_START) / DISTRIBUTION_STEP);
    private static final String logAccount = Conch.getStringProperty("sharder.logFxtBalance");
    private static final long logAccountId = Account.rsAccountToId(logAccount);
    private static final String fxtJsonFile = Constants.isTestnetOrDevnet() ? "fxt-testnet.json" : "fxt.json";
    private static final boolean hasSnapshot = ClassLoader.getSystemResource(fxtJsonFile) != null;

    private static final DerivedDbTable accountFXTTable = new DerivedDbTable("account_fxt") {
        @Override
        public void trim(int height) {
            try (Connection con = db.getConnection()) {
                if (height > DISTRIBUTION_END) {
                    try (Statement stmt = con.createStatement()) {
                        stmt.executeUpdate("TRUNCATE TABLE account_fxt");
                    }
                } else {
                    try (PreparedStatement pstmtCreate = con.prepareStatement("CREATE TEMP TABLE account_fxt_tmp NOT PERSISTENT AS "
                            + "SELECT id, MAX(height) AS height FROM account_fxt WHERE height < ? GROUP BY id");
                         PreparedStatement pstmtDrop = con.prepareStatement("DROP TABLE account_fxt_tmp")) {
                        pstmtCreate.setInt(1, height);
                        pstmtCreate.executeUpdate();
                        try (PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_fxt WHERE (id, height) NOT IN "
                                + "(SELECT (id, height) FROM account_fxt_tmp) AND height < ? AND height >= 0")) {
                            pstmt.setInt(1, height);
                            pstmt.executeUpdate();
                        } finally {
                            pstmtDrop.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    };

    public static void init() {}
    // close dividend distribution
    final static boolean fxtOpen = false;
    
    static {
        if(fxtOpen) {
            Conch.getBlockchainProcessor().addListener(new FxtDistribution(), BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
        }
    }

    static int debugCount = 0;
    @Override
    public void notify(Block block) {
        if(!fxtOpen){
            if(debugCount++ < 2) Logger.logDebugMessage("Close FXT Distributing Now");
            return;
        }

        final int currentHeight = block.getHeight();
        if (hasSnapshot) {
            if (currentHeight == DISTRIBUTION_END) {
                Logger.logDebugMessage("Distributing FXT based on snapshot file " + fxtJsonFile);
                JSONObject snapshotJSON;
                try (Reader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(fxtJsonFile)))) {
                    snapshotJSON = (JSONObject) JSONValue.parse(reader);
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                boolean wasInTransaction = db.isInTransaction();
                if (!wasInTransaction) {
                    db.beginTransaction();
                }
                try {
                    long initialQuantity = Asset.getAsset(FXT_ASSET_ID).getInitialQuantityQNT();
                    Account issuerAccount = Account.getAccount(FXT_ISSUER_ID);
                    issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                            FXT_ASSET_ID, -initialQuantity);
                    long totalDistributed = 0;
                    Iterator<Map.Entry> iterator = snapshotJSON.entrySet().iterator();
                    int count = 0;
                    while (iterator.hasNext()) {
                        Map.Entry entry = iterator.next();
                        long accountId = Long.parseUnsignedLong((String) entry.getKey());
                        long quantity = (Long)entry.getValue();
                        Account.getAccount(accountId).addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                                FXT_ASSET_ID, quantity);
                        totalDistributed += quantity;
                        if (++count % 1000 == 0) {
                            db.commitTransaction();
                        }
                    }
                    long excessFxtQuantity = initialQuantity - totalDistributed;
                    Asset.deleteAsset(TransactionDb.findTransaction(FXT_ASSET_ID), FXT_ASSET_ID, excessFxtQuantity);
                    Logger.logDebugMessage("Deleted " + excessFxtQuantity + " excess QNT");
                    Logger.logDebugMessage("Distributed " + totalDistributed + " QNT to " + count + " accounts");
                    db.commitTransaction();
                } catch (Exception e) {
                    db.rollbackTransaction();
                    throw new RuntimeException(e.toString(), e);
                } finally {
                    if (!wasInTransaction) {
                        db.endTransaction();
                    }
                }
            }
            return;
        }
        if (currentHeight <= DISTRIBUTION_START || currentHeight > DISTRIBUTION_END || (currentHeight - DISTRIBUTION_START) % DISTRIBUTION_FREQUENCY != 0) {
            return;
        }
        Logger.logDebugMessage("Running FXT balance update at height " + currentHeight);
        Map<Long, BigInteger> accountBalanceTotals = new HashMap<>();
        for (int height = currentHeight - DISTRIBUTION_FREQUENCY + DISTRIBUTION_STEP; height <= currentHeight; height += DISTRIBUTION_STEP) {
            Logger.logDebugMessage("Calculating balances at height " + height);
            try (Connection con = db.getConnection();
                 PreparedStatement pstmtCreate = con.prepareStatement("CREATE TEMP TABLE account_tmp NOT PERSISTENT AS SELECT id, MAX(height) as height FROM account "
                         + "WHERE height <= ? GROUP BY id")) {
                pstmtCreate.setInt(1, height);
                pstmtCreate.executeUpdate();
                try (PreparedStatement pstmtSelect = con.prepareStatement("SELECT account.id, account.balance FROM account, account_tmp WHERE account.id = account_tmp.id "
                        + "AND account.height = account_tmp.height AND account.balance > 0");
                     PreparedStatement pstmtDrop = con.prepareStatement("DROP TABLE account_tmp")) {
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            Long accountId = rs.getLong("id");
                            long balance = rs.getLong("balance");
                            if (logAccountId != 0) {
                                if (accountId == logAccountId) {
                                    Logger.logMessage("CDWH balance for " + logAccount + " at height " + height + ":\t" + balance);
                                }
                            }
                            BigInteger accountBalanceTotal = accountBalanceTotals.get(accountId);
                            accountBalanceTotals.put(accountId, accountBalanceTotal == null ?
                                    BigInteger.valueOf(balance) : accountBalanceTotal.add(BigInteger.valueOf(balance)));
                        }
                    } finally {
                        pstmtDrop.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        Logger.logDebugMessage("Updating balances for " + accountBalanceTotals.size() + " accounts");
        boolean wasInTransaction = db.isInTransaction();
        if (!wasInTransaction) {
            db.beginTransaction();
        }
        db.clearCache();
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT balance FROM account_fxt WHERE id = ? ORDER BY height DESC LIMIT 1");
             PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account_fxt (id, balance, height) values (?, ?, ?)")) {
            int count = 0;
            for (Map.Entry<Long, BigInteger> entry : accountBalanceTotals.entrySet()) {
                long accountId = entry.getKey();
                BigInteger balanceTotal = entry.getValue();
                pstmtSelect.setLong(1, accountId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        balanceTotal = balanceTotal.add(new BigInteger(rs.getBytes("balance")));
                    }
                }
                if (logAccountId != 0) {
                    if (accountId == logAccountId) {
                        Logger.logMessage("Average CDWH balance for " + logAccount + " as of height " + currentHeight + ":\t"
                                + balanceTotal.divide(BigInteger.valueOf((currentHeight - DISTRIBUTION_START) / DISTRIBUTION_STEP)).longValueExact());
                    }
                }
                pstmtInsert.setLong(1, accountId);
                pstmtInsert.setBytes(2, balanceTotal.toByteArray());
                pstmtInsert.setInt(3, currentHeight);
                pstmtInsert.executeUpdate();
                if (++count % 1000 == 0) {
                    db.commitTransaction();
                }
            }
            accountBalanceTotals.clear();
            db.commitTransaction();
            if (currentHeight == DISTRIBUTION_END) {
                Logger.logDebugMessage("Running FXT distribution at height " + currentHeight);
                long totalDistributed = 0;
                count = 0;
                SortedMap<String, Long> snapshotMap = new TreeMap<>();
                try (PreparedStatement pstmtCreate = con.prepareStatement("CREATE TEMP TABLE account_fxt_tmp NOT PERSISTENT AS SELECT id, MAX(height) AS height FROM account_fxt "
                        + "WHERE height <= ? GROUP BY id");
                     PreparedStatement pstmtDrop = con.prepareStatement("DROP TABLE account_fxt_tmp")) {
                    pstmtCreate.setInt(1, currentHeight);
                    pstmtCreate.executeUpdate();
                    try (PreparedStatement pstmt = con.prepareStatement("SELECT account_fxt.id, account_fxt.balance FROM account_fxt, account_fxt_tmp "
                            + "WHERE account_fxt.id = account_fxt_tmp.id AND account_fxt.height = account_fxt_tmp.height");
                         ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long accountId = rs.getLong("id");
                            // 1 CDWH held for the full period should give 1 asset unit, i.e. 10000 QNT assuming 4 decimals
                            long quantity = new BigInteger(rs.getBytes("balance")).divide(BALANCE_DIVIDER).longValueExact();
                            if (logAccountId != 0) {
                                if (accountId == logAccountId) {
                                    Logger.logMessage("FXT quantity for " + logAccount + ":\t" + quantity);
                                }
                            }
                            Account.getAccount(accountId).addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                                    FXT_ASSET_ID, quantity);
                            snapshotMap.put(Long.toUnsignedString(accountId), quantity);
                            totalDistributed += quantity;
                            if (++count % 1000 == 0) {
                                db.commitTransaction();
                            }
                        }
                    } finally {
                        pstmtDrop.executeUpdate();
                    }
                }
                Account issuerAccount = Account.getAccount(FXT_ISSUER_ID);
                issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                        FXT_ASSET_ID, -totalDistributed);
                long excessFxtQuantity = Asset.getAsset(FXT_ASSET_ID).getInitialQuantityQNT() - totalDistributed;
                issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(null, block.getId(),
                        FXT_ASSET_ID, -excessFxtQuantity);
                long issuerAssetBalance = issuerAccount.getAssetBalanceQNT(FXT_ASSET_ID);
                if (issuerAssetBalance > 0) {
                    snapshotMap.put(Long.toUnsignedString(FXT_ISSUER_ID), issuerAssetBalance);
                } else {
                    snapshotMap.remove(Long.toUnsignedString(FXT_ISSUER_ID));
                }
                Asset.deleteAsset(TransactionDb.findTransaction(FXT_ASSET_ID), FXT_ASSET_ID, excessFxtQuantity);
                Logger.logDebugMessage("Deleted " + excessFxtQuantity + " excess QNT");
                Logger.logDebugMessage("Distributed " + totalDistributed + " QNT to " + count + " accounts");
                try (PrintWriter writer = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fxtJsonFile)))), true)) {
                    StringBuilder sb = new StringBuilder(1024);
                    JSON.encodeObject(snapshotMap, sb);
                    writer.write(sb.toString());
                }
                db.commitTransaction();
            }
        } catch (Exception e) {
            db.rollbackTransaction();
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (!wasInTransaction) {
                db.endTransaction();
            }
        }
    }
}
