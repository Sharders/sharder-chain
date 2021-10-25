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

package org.conch.peer;

import com.google.common.collect.Lists;
import org.conch.account.Account;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author bowen
 * @date 2021/03/05
 */
public final class ForkBlock {

    public static final class ForkBlockObj {
        private long blockId;
        private List<ForkBlock> forkBlocks;
        private List<String> generators = Lists.newArrayList();

        public ForkBlockObj(long blockId, List<ForkBlock> forkBlocks, String generator) {
            this.blockId = blockId;
            ArrayList<ForkBlock> list = Lists.newArrayList();
            for (ForkBlock forkBlock : forkBlocks) {
                forkBlock.setGeneratorRS(Account.rsAccount(forkBlock.getGeneratorId()));
                list.add(forkBlock);
            }
            this.forkBlocks = list;
            this.generators.add(generator);
        }

        public long getBlockId() {
            return blockId;
        }

        public void setBlockId(long blockId) {
            this.blockId = blockId;
        }

        public List<ForkBlock> getForkBlocks() {
            return forkBlocks;
        }

        public void setForkBlocks(List<ForkBlock> forkBlocks) {
            ArrayList<ForkBlock> list = Lists.newArrayList();
            for (ForkBlock forkBlock : forkBlocks) {
                forkBlock.setGeneratorRS(Account.rsAccount(forkBlock.getGeneratorId()));
                list.add(forkBlock);
            }
            this.forkBlocks = list;
        }

        public List<String> getGenerators() {
            return generators;
        }

        public void setGenerators(List<String> generators) {
            this.generators = generators;
        }

        public void addGenerator(String generator) {
            this.generators.add(generator);
        }

        public void deleteGenerator(String generator) {
            this.generators.remove(generator);
        }
    }

    public static final class ForkBlockLinkedAccount {
        private long blockId;
        private long accountId;
        private int height;

        public ForkBlockLinkedAccount(long blockId, long accountId, int height) {
            this.blockId = blockId;
            this.accountId = accountId;
            this.height = height;
        }

        public long getBlockId() {
            return blockId;
        }

        public void setBlockId(long blockId) {
            this.blockId = blockId;
        }

        public long getAccountId() {
            return accountId;
        }

        public void setAccountId(long accountId) {
            this.accountId = accountId;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForkBlockLinkedAccount that = (ForkBlockLinkedAccount) o;
            return getBlockId() == that.getBlockId() && getAccountId() == that.getAccountId() && getHeight() == that.getHeight();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getBlockId(), getAccountId());
        }

        @Override
        public String toString() {
            return "ForkBlockLinkedAccount{" +
                    "blockId=" + blockId +
                    ", accountId=" + accountId +
                    ", height=" + height +
                    '}';
        }
    }

    private final int version;
    private final int timestamp;
    private long previousBlockId;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile long generatorId;
    private String generatorRS;

    public ForkBlock(int version, int timestamp, long id, long generatorId, BigInteger cumulativeDifficulty, int height) {
        this.version = version;
        this.timestamp = timestamp;
        this.id = id;
        this.generatorId = generatorId;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.height = height;
    }

    public int getVersion() {
        return version;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public long getPreviousBlockId() {
        return previousBlockId;
    }

    public void setPreviousBlockId(long previousBlockId) {
        this.previousBlockId = previousBlockId;
    }

    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    public long getNextBlockId() {
        return nextBlockId;
    }

    public void setNextBlockId(long nextBlockId) {
        this.nextBlockId = nextBlockId;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGeneratorId() {
        return generatorId;
    }

    public void setGeneratorId(long generatorId) {
        this.generatorId = generatorId;
    }

    public String getGeneratorRS() {
        return generatorRS;
    }

    public void setGeneratorRS(String generatorRS) {
        this.generatorRS = generatorRS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForkBlock forkBlock = (ForkBlock) o;
        return getId() == forkBlock.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }


}
