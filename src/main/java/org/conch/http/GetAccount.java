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

package org.conch.http;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocScore;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.peer.CertifiedPeer;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class GetAccount extends APIServlet.APIRequestHandler {

    static final GetAccount instance = new GetAccount();

    private GetAccount() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "includeLessors", "includeAssets", "includeCurrencies", "includeEffectiveBalance");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        if(req.getParameter("allIncome") != null) {
            return getCutIncome();
        }
        Account account = ParameterParser.getAccount(req);
        boolean includeLessors = "true".equalsIgnoreCase(req.getParameter("includeLessors"));
        boolean includeAssets = "true".equalsIgnoreCase(req.getParameter("includeAssets"));
        boolean includeCurrencies = "true".equalsIgnoreCase(req.getParameter("includeCurrencies"));
        boolean includeEffectiveBalance = "true".equalsIgnoreCase(req.getParameter("includeEffectiveBalance"));

        JSONObject response = JSONData.accountBalance(account, includeEffectiveBalance);
        JSONData.putAccount(response, "account", account.getId());

        byte[] publicKey = Account.getPublicKey(account.getId());
        if (publicKey != null) {
            response.put("publicKey", Convert.toHexString(publicKey));
        }
        Account.AccountInfo accountInfo = account.getAccountInfo();
        if (accountInfo != null) {
            response.put("name", Convert.nullToEmpty(accountInfo.getName()));
            response.put("description", Convert.nullToEmpty(accountInfo.getDescription()));
        }
        Account.AccountLease accountLease = account.getAccountLease();
        if (accountLease != null) {
            JSONData.putAccount(response, "currentLessee", accountLease.getCurrentLesseeId());
            response.put("currentLeasingHeightFrom", accountLease.getCurrentLeasingHeightFrom());
            response.put("currentLeasingHeightTo", accountLease.getCurrentLeasingHeightTo());
            if (accountLease.getNextLesseeId() != 0) {
                JSONData.putAccount(response, "nextLessee", accountLease.getNextLesseeId());
                response.put("nextLeasingHeightFrom", accountLease.getNextLeasingHeightFrom());
                response.put("nextLeasingHeightTo", accountLease.getNextLeasingHeightTo());
            }
        }

        if (!account.getControls().isEmpty()) {
            JSONArray accountControlsJson = new JSONArray();
            account.getControls().forEach(accountControl -> accountControlsJson.add(accountControl.toString()));
            response.put("accountControls", accountControlsJson);
        }

        if (includeLessors) {
            DbIterator<Account> lessors = null;
            try {
                lessors = account.getLessors();
                if (lessors.hasNext()) {
                    JSONArray lessorIds = new JSONArray();
                    JSONArray lessorIdsRS = new JSONArray();
                    JSONArray lessorInfo = new JSONArray();
                    while (lessors.hasNext()) {
                        Account lessor = lessors.next();
                        lessorIds.add(Long.toUnsignedString(lessor.getId()));
                        lessorIdsRS.add(Account.rsAccount(lessor.getId()));
                        lessorInfo.add(JSONData.lessor(lessor, includeEffectiveBalance));
                    }
                    response.put("lessors", lessorIds);
                    response.put("lessorsRS", lessorIdsRS);
                    response.put("lessorsInfo", lessorInfo);
                }
            }finally {
                DbUtils.close(lessors);
            }
        }

        if (includeAssets) {
            DbIterator<Account.AccountAsset> accountAssets = null;
            try {
                accountAssets = account.getAssets(0, -1);
                JSONArray assetBalances = new JSONArray();
                JSONArray unconfirmedAssetBalances = new JSONArray();
                while (accountAssets.hasNext()) {
                    Account.AccountAsset accountAsset = accountAssets.next();
                    JSONObject assetBalance = new JSONObject();
                    assetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                    assetBalance.put("balanceQNT", String.valueOf(accountAsset.getQuantityQNT()));
                    assetBalances.add(assetBalance);
                    JSONObject unconfirmedAssetBalance = new JSONObject();
                    unconfirmedAssetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                    unconfirmedAssetBalance.put("unconfirmedBalanceQNT", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
                    unconfirmedAssetBalances.add(unconfirmedAssetBalance);
                }
                if (assetBalances.size() > 0) {
                    response.put("assetBalances", assetBalances);
                }
                if (unconfirmedAssetBalances.size() > 0) {
                    response.put("unconfirmedAssetBalances", unconfirmedAssetBalances);
                }
            }finally {
                DbUtils.close(accountAssets);
            }
        }

        if (includeCurrencies) {
            DbIterator<Account.AccountCurrency> accountCurrencies = null;
            try {
                accountCurrencies = account.getCurrencies(0, -1);
                JSONArray currencyJSON = new JSONArray();
                while (accountCurrencies.hasNext()) {
                    currencyJSON.add(JSONData.accountCurrency(accountCurrencies.next(), false, true));
                }
                if (currencyJSON.size() > 0) {
                    response.put("accountCurrencies", currencyJSON);
                }
            }finally {
                DbUtils.close(accountCurrencies);
            }
        }

        // poc score
        try{
            PocScore scoreObj = Conch.getPocProcessor().calPocScore(account, Conch.getHeight());
            if (scoreObj.getTotal() == null) {
                scoreObj.setTotal(0L);
            }
            response.put("pocScore", com.alibaba.fastjson.JSONObject.toJSON(scoreObj));

            CertifiedPeer certifiedPeer = Conch.getPocProcessor().getCertifiedPeers().get(account.getId());
            if(certifiedPeer != null){
                response.put("nodeType", certifiedPeer.getType());
                response.put("declaredTime",  certifiedPeer.getUpdateTime());
            }else{
                response.put("nodeType", "Unknown");
                response.put("declaredTime", "");
            }
        }catch(Exception e){
            Logger.logErrorMessage("can't get the poc score", e);
        }

        return response;

    }

    private JSONObject getCutIncome(){
        JSONObject json = new JSONObject();
        json.put("success",true);
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT sum(a.FORGED_BALANCE) as num from ACCOUNT as a where a.DB_ID in (select max(DB_ID) from ACCOUNT as ma where a.ID = ma.ID)");
            json.put("cutIncome",GetAccountRanking.result(ps.executeQuery()));
            con.commit();
            con.close();
        } catch (SQLException e) {
            json.put("success",false);
            e.printStackTrace();
        }finally {
            DbUtils.close(con);
        }
        return json;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }
}
