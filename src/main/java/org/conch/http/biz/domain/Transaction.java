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
 * Transaction
 *
 * @author bubai
 * @date 2018/4/2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Transaction {
    private String transactionId;
    private String hash;
    private int type;
    private int index;
    private BigDecimal amount;
    private BigDecimal fee;
    private String sender;
    private String recipient;
    private int height;
    private long timestamp;
    private int confirmations;
    private Data attachment;

    @JsonProperty(value = "transactionId")
    public String getTransactionId() {
        return transactionId;
    }

    @JsonProperty(value = "transaction")
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @JsonProperty(value = "hash")
    public String getHash() {
        return hash;
    }

    @JsonProperty(value = "fullHash")
    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @JsonProperty(value = "index")
    public int getIndex() {
        return index;
    }

    @JsonProperty(value = "transactionIndex")
    public void setIndex(int index) {
        this.index = index;
    }

    @JsonProperty(value = "amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty(value = "amountNQT")
    public void setAmount(BigDecimal amount) {
        this.amount = amount.divide(BigDecimal.valueOf(100000000L));
    }

    @JsonProperty(value = "fee")
    public BigDecimal getFee() {
        return fee;
    }

    @JsonProperty(value = "feeNQT")
    public void setFee(BigDecimal fee) {
        this.fee = fee.divide(BigDecimal.valueOf(100000000L));
    }

    @JsonProperty(value = "sender")
    public String getSender() {
        return sender;
    }

    @JsonProperty(value = "senderRS")
    public void setSender(String sender) {
        this.sender = sender;
    }

    @JsonProperty(value = "recipient")
    public String getRecipient() {
        return recipient;
    }

    @JsonProperty(value = "recipientRS")
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    @JsonProperty(value = "data")
    public Data getAttachment() {
        return attachment;
    }
    
    @JsonProperty(value = "attachment")
    public void setAttachment(Data attachment) {
        if (type == 6) {
            this.attachment = attachment;
        }
    }
}
