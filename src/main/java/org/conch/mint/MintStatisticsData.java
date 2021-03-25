package org.conch.mint;

import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.consensus.poc.PocHolder;
import org.conch.consensus.poc.PocScore;
import org.conch.peer.CertifiedPeer;

import java.math.BigDecimal;

public class MintStatisticsData {

    private Long minerId;
    private Integer generateCount;
    private BigDecimal generateRate;
    private int latestMiningTime;
    private Integer avgMiningTime;
    private PocScore pocScore;
    private String miningMachineIP;
    private String noteType;
    private int height;

    public MintStatisticsData(Long minerId, int latestMiningTime) {
        this.minerId = minerId;
        this.latestMiningTime = latestMiningTime;
    }

    /**
     * count miner data first time, init MintStatistics instance
     *
     * @param generatorId
     * @param timestamp
     * @return
     */
    public static MintStatisticsData init(long generatorId, int timestamp, int startHeight, int endHeight) {
        MintStatisticsData mintStatisticsData = new MintStatisticsData(generatorId, timestamp);
        PocScore pocScore = PocHolder.getPocScore(Conch.getBlockchain().getHeight(), generatorId);
        mintStatisticsData.setPocScore(pocScore);
        CertifiedPeer certifiedPeer = Conch.getPocProcessor().getCertifiedPeers().get(generatorId);
        mintStatisticsData.setMiningMachineIP(certifiedPeer.getIp());
        mintStatisticsData.setNoteType(certifiedPeer.getType().getName());
        mintStatisticsData.setGenerateCount(1);
        mintStatisticsData.setGenerateRate(new BigDecimal("1").divide(new BigDecimal(endHeight - startHeight + ""), BigDecimal.ROUND_HALF_UP, 6));
        mintStatisticsData.setHeight(endHeight);
        return mintStatisticsData;
    }

    /**
     * calculate and add up miner data
     *
     * @param block
     */
    public void updateData(Block block, int startHeight, int endHeight) {
        if (avgMiningTime != null) {
            this.avgMiningTime = ((avgMiningTime * generateCount) + (block.getTimestamp() - this.latestMiningTime)) / (generateCount + 1);
        } else if (avgMiningTime == null && generateCount < 2) {
            this.avgMiningTime = (block.getTimestamp() - this.latestMiningTime) / 2;
        }
        this.latestMiningTime = block.getTimestamp();
        this.generateCount = generateCount + 1;
        this.generateRate = new BigDecimal(generateCount).divide(new BigDecimal(endHeight - startHeight + ""), BigDecimal.ROUND_HALF_UP, 6);
        this.setHeight(block.getHeight());
    }

    public Long getMinerId() {
        return minerId;
    }

    public void setMinerId(Long minerId) {
        this.minerId = minerId;
    }

    public Integer getGenerateCount() {
        return generateCount;
    }

    public void setGenerateCount(Integer generateCount) {
        this.generateCount = generateCount;
    }

    public BigDecimal getGenerateRate() {
        return generateRate;
    }

    public void setGenerateRate(BigDecimal generateRate) {
        this.generateRate = generateRate;
    }

    public int getLatestMiningTime() {
        return latestMiningTime;
    }

    public void setLatestMiningTime(int latestMiningTime) {
        this.latestMiningTime = latestMiningTime;
    }

    public Integer getAvgMiningTime() {
        return avgMiningTime;
    }

    public void setAvgMiningTime(Integer avgMiningTime) {
        this.avgMiningTime = avgMiningTime;
    }

    public PocScore getPocScore() {
        return pocScore;
    }

    public void setPocScore(PocScore pocScore) {
        this.pocScore = pocScore;
    }

    public String getMiningMachineIP() {
        return miningMachineIP;
    }

    public void setMiningMachineIP(String miningMachineIP) {
        this.miningMachineIP = miningMachineIP;
    }

    public String getNoteType() {
        return noteType;
    }

    public void setNoteType(String noteType) {
        this.noteType = noteType;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
