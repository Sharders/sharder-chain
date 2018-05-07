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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Block
 *
 * @author bubai
 * @date 2018/4/2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Block {
    private String blockId;
    private String previousBlockId;
    private int height;
    private int payloadLength;
    private long timestamp;
    private String generator;
    private String generatorRS;
    private ArrayList<Transaction> transactions;
    private BigDecimal totalFee;
    private int numberOfTransactions;
    private BigDecimal totalAmount;

    @JsonProperty(value = "blockId", index = 0)
    public String getBlockId() {
        return blockId;
    }

    @JsonProperty(value = "block")
    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    @JsonProperty(value = "previousBlockId")
    public String getPreviousBlockId() {
        return previousBlockId;
    }

    @JsonProperty(value = "previousBlock")
    public void setPreviousBlockId(String previousBlockId) {
        this.previousBlockId = previousBlockId;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getGeneratorRS() {
        return generatorRS;
    }

    public void setGeneratorRS(String generatorRS) {
        this.generatorRS = generatorRS;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }

    @JsonProperty(value = "totalFee")
    public BigDecimal getTotalFee() {
        return totalFee;
    }

    @JsonProperty(value = "totalFeeNQT")
    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee.divide(BigDecimal.valueOf(100000000L));;
    }

    public int getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public void setNumberOfTransactions(int numberOfTransactions) {
        this.numberOfTransactions = numberOfTransactions;
    }

    @JsonProperty(value = "totalAmount")
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    @JsonProperty(value = "totalAmountNQT")
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount.divide(BigDecimal.valueOf(100000000L));
    }
}
