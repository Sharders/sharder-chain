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

package org.conch.http.biz.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Account
 *
 * @author bubai
 * @date 2018/3/23
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Account {
    private String accountID;
    private String accountRS;
    private String publicKey;
    private String passPhrase;
    private BigDecimal forgedBalance;
    private BigDecimal balance;

    public Account() {
    }
    public Account(String accountID) {
        this.accountID = accountID;
    }


    @JsonProperty(value = "accountId")
    public String getAccountID() {
        return accountID;
    }

    @JsonProperty(value = "account")
    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPassPhrase() {
        return passPhrase;
    }

    public void setPassPhrase(String passPhrase) {
        this.passPhrase = passPhrase;
    }

    @JsonProperty(value = "forgedBalance")
    public BigDecimal getForgedBalance() {
        return forgedBalance;
    }

    @JsonProperty(value = "forgedBalanceNQT")
    public void setForgedBalance(BigDecimal forgedBalance) {
        this.forgedBalance = forgedBalance.divide(BigDecimal.valueOf(100000000L));
    }

    @JsonProperty(value = "balance")
    public BigDecimal getBalance() {
        return balance;
    }

    @JsonProperty(value = "balanceNQT")
    public void setBalance(BigDecimal balance) {
        this.balance = balance.divide(BigDecimal.valueOf(100000000L));
    }
}
