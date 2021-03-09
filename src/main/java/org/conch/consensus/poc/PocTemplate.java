package org.conch.consensus.poc;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PocTemplate implements Serializable {

  // 用户公钥
  private String userPublicKey;

  // 访问节点
  private String ip;
  private Integer port;

  // 权重项的占比
  private Long nodeTypeWeight;
  private Long serverOpenWeight;
  private Long ssHoldWeight;
  // 磁盘容量
  private Long hardwareConfWeight;
  private Long netWorkConfWeight;
  private Long txHandlePerformanceWeight;

  // 节点类型分数
  private Long foundationNodeScore;
  private Long communityNodeScore;
  private Long hubNodeScore;
  private Long BoxNodeScore;
  private Long NormalNodeScore;

  // 开启服务分数
  private Long minerScore;
  private Long bapiScore;
  private Long naterScore;
  private Long storageScore;
  private Long proverScore;

  // 交易处理性能分数
  private Long badTxScore;
  private Long middleTxScore;
  private Long goodTxScore;

  // 硬件配置分数
  private Long badHardwareScore;
  private Long middleHardwareScore;
  private Long goodHardwareScore;

  // 网络配置分数
  private Long poorNetworkScore;
  private Long badNetworkScore;
  private Long middleNetworkScore;
  private Long goodNetworkScore;

  // 出块错过惩罚表
  private Long badBlockingMissScore;
  private Long middleBlockingMissScore;
  private Long goodBlockingMissScore;

  // 分叉收敛惩罚表
  private Long badBocSpeedScore;
  private Long middleBocSpeedScore;
  private Long poorBocSpeedScore;

  // 在线率奖惩表
  // 基金会在线率
  private Long foundationFrom9900To9999;
  private Long foundationFrom9700To9900;
  private Long foundationFrom0000To9700;
  // 社区在线率
  private Long communityFrom9700To9900;
  private Long communityFrom9000To9700;
  private Long communityFrom0000To9000;
  // Hub或Box在线率
  private Long hbFrom9900To100;
  private Long hbFrom9700To100;
  private Long hbFrom0000To9000;
  // 普通在线率
  private Long normalFrom9700To100;
  private Long normalFrom9000To100;

  // PoC模板版本
  private Long version;

  public PocTemplate() {
    this("", "", 0);
  }

  // 默认权重表配置
  public PocTemplate(String userPublicKey, String ip, Integer port) {
    this.userPublicKey = userPublicKey;
    this.ip = ip;
    this.port = port;

    // rate definitions
    this.nodeTypeWeight = 10L;
    this.ssHoldWeight = 45L;
    this.hardwareConfWeight = 32L;
    this.netWorkConfWeight = 8L;
    this.txHandlePerformanceWeight = 5L;

    this.foundationNodeScore = 10L;
    this.communityNodeScore = 10L;
    this.BoxNodeScore = 8L;
    this.hubNodeScore = 6L;
    this.NormalNodeScore = 3L;

    this.minerScore = 4L;
    this.bapiScore = 4L;
    this.naterScore = 4L;
    this.storageScore = 4L;
    this.proverScore = 4L;

    this.badTxScore = 6L;
    this.middleTxScore = 8L;
    this.goodTxScore = 10L;

    this.badHardwareScore = 3L;
    this.middleHardwareScore = 6L;
    this.goodHardwareScore = 10L;

    this.poorNetworkScore = 0L;
    this.badNetworkScore = 6L;
    this.middleNetworkScore = 8L;
    this.goodNetworkScore = 10L;

    this.badBlockingMissScore = -10L;
    this.middleBlockingMissScore = -6L;
    this.goodBlockingMissScore = -3L;

    this.badBocSpeedScore = -3L;
    this.middleBocSpeedScore = -6L;
    this.poorBocSpeedScore = -10L;

    this.foundationFrom9900To9999 = -2L;
    this.foundationFrom9700To9900 = -5L;
    this.foundationFrom0000To9700 = -10L;
    this.communityFrom9700To9900 = -2L;
    this.communityFrom9000To9700 = -5L;
    this.communityFrom0000To9000 = -10L;
    this.hbFrom9900To100 = 5L;
    this.hbFrom9700To100 = 3L;
    this.hbFrom0000To9000 = -5L;
    this.normalFrom9700To100 = 5L;
    this.normalFrom9000To100 = 3L;

    this.version = Long.valueOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
  }

  public String getUserPublicKey() {
    return userPublicKey;
  }

  public PocTemplate setUserPublicKey(String userPublicKey) {
    this.userPublicKey = userPublicKey;
    return this;
  }

  public String getIp() {
    return ip;
  }

  public PocTemplate setIp(String ip) {
    this.ip = ip;
    return this;
  }

  public Integer getPort() {
    return port;
  }

  public PocTemplate setPort(Integer port) {
    this.port = port;
    return this;
  }

  public Long getNodeTypeWeight() {
    return nodeTypeWeight;
  }

  public PocTemplate setNodeTypeWeight(Long nodeTypeWeight) {
    this.nodeTypeWeight = nodeTypeWeight;
    return this;
  }

  public Long getServerOpenWeight() {
    return serverOpenWeight;
  }

  public PocTemplate setServerOpenWeight(Long serverOpenWeight) {
    this.serverOpenWeight = serverOpenWeight;
    return this;
  }

  public Long getSsHoldWeight() {
    return ssHoldWeight;
  }

  public PocTemplate setSsHoldWeight(Long ssHoldWeight) {
    this.ssHoldWeight = ssHoldWeight;
    return this;
  }

  public Long getHardwareConfWeight() {
    return hardwareConfWeight;
  }

  public PocTemplate setHardwareConfWeight(Long hardwareConfWeight) {
    this.hardwareConfWeight = hardwareConfWeight;
    return this;
  }

  public Long getNetWorkConfWeight() {
    return netWorkConfWeight;
  }

  public PocTemplate setNetWorkConfWeight(Long netWorkConfWeight) {
    this.netWorkConfWeight = netWorkConfWeight;
    return this;
  }

  public Long getTxHandlePerformanceWeight() {
    return txHandlePerformanceWeight;
  }

  public PocTemplate setTxHandlePerformanceWeight(Long txHandlePerformanceWeight) {
    this.txHandlePerformanceWeight = txHandlePerformanceWeight;
    return this;
  }

  public Long getFoundationNodeScore() {
    return foundationNodeScore;
  }

  public PocTemplate setFoundationNodeScore(Long foundationNodeScore) {
    this.foundationNodeScore = foundationNodeScore;
    return this;
  }

  public Long getCommunityNodeScore() {
    return communityNodeScore;
  }

  public PocTemplate setCommunityNodeScore(Long communityNodeScore) {
    this.communityNodeScore = communityNodeScore;
    return this;
  }

  public Long getHubNodeScore() {
    return hubNodeScore;
  }

  public PocTemplate setHubNodeScore(Long hubNodeScore) {
    this.hubNodeScore = hubNodeScore;
    return this;
  }

  public Long getBoxNodeScore() {
    return BoxNodeScore;
  }

  public PocTemplate setBoxNodeScore(Long boxNodeScore) {
    BoxNodeScore = boxNodeScore;
    return this;
  }

  public Long getNormalNodeScore() {
    return NormalNodeScore;
  }

  public PocTemplate setNormalNodeScore(Long normalNodeScore) {
    NormalNodeScore = normalNodeScore;
    return this;
  }

  public Long getMinerScore() {
    return minerScore;
  }

  public PocTemplate setMinerScore(Long minerScore) {
    this.minerScore = minerScore;
    return this;
  }

  public Long getBapiScore() {
    return bapiScore;
  }

  public PocTemplate setBapiScore(Long bapiScore) {
    this.bapiScore = bapiScore;
    return this;
  }

  public Long getNaterScore() {
    return naterScore;
  }

  public PocTemplate setNaterScore(Long naterScore) {
    this.naterScore = naterScore;
    return this;
  }

  public Long getStorageScore() {
    return storageScore;
  }

  public PocTemplate setStorageScore(Long storageScore) {
    this.storageScore = storageScore;
    return this;
  }

  public Long getProverScore() {
    return proverScore;
  }

  public PocTemplate setProverScore(Long proverScore) {
    this.proverScore = proverScore;
    return this;
  }

  public Long getBadTxScore() {
    return badTxScore;
  }

  public PocTemplate setBadTxScore(Long badTxScore) {
    this.badTxScore = badTxScore;
    return this;
  }

  public Long getMiddleTxScore() {
    return middleTxScore;
  }

  public PocTemplate setMiddleTxScore(Long middleTxScore) {
    this.middleTxScore = middleTxScore;
    return this;
  }

  public Long getGoodTxScore() {
    return goodTxScore;
  }

  public PocTemplate setGoodTxScore(Long goodTxScore) {
    this.goodTxScore = goodTxScore;
    return this;
  }

  public Long getBadHardwareScore() {
    return badHardwareScore;
  }

  public PocTemplate setBadHardwareScore(Long badHardwareScore) {
    this.badHardwareScore = badHardwareScore;
    return this;
  }

  public Long getMiddleHardwareScore() {
    return middleHardwareScore;
  }

  public PocTemplate setMiddleHardwareScore(Long middleHardwareScore) {
    this.middleHardwareScore = middleHardwareScore;
    return this;
  }

  public Long getGoodHardwareScore() {
    return goodHardwareScore;
  }

  public PocTemplate setGoodHardwareScore(Long goodHardwareScore) {
    this.goodHardwareScore = goodHardwareScore;
    return this;
  }

  public Long getPoorNetworkScore() {
    return poorNetworkScore;
  }

  public PocTemplate setPoorNetworkScore(Long poorNetworkScore) {
    this.poorNetworkScore = poorNetworkScore;
    return this;
  }

  public Long getBadNetworkScore() {
    return badNetworkScore;
  }

  public PocTemplate setBadNetworkScore(Long badNetworkScore) {
    this.badNetworkScore = badNetworkScore;
    return this;
  }

  public Long getMiddleNetworkScore() {
    return middleNetworkScore;
  }

  public PocTemplate setMiddleNetworkScore(Long middleNetworkScore) {
    this.middleNetworkScore = middleNetworkScore;
    return this;
  }

  public Long getGoodNetworkScore() {
    return goodNetworkScore;
  }

  public PocTemplate setGoodNetworkScore(Long goodNetworkScore) {
    this.goodNetworkScore = goodNetworkScore;
    return this;
  }

  public Long getBadBlockingMissScore() {
    return badBlockingMissScore;
  }

  public PocTemplate setBadBlockingMissScore(Long badBlockingMissScore) {
    this.badBlockingMissScore = badBlockingMissScore;
    return this;
  }

  public Long getMiddleBlockingMissScore() {
    return middleBlockingMissScore;
  }

  public PocTemplate setMiddleBlockingMissScore(Long middleBlockingMissScore) {
    this.middleBlockingMissScore = middleBlockingMissScore;
    return this;
  }

  public Long getGoodBlockingMissScore() {
    return goodBlockingMissScore;
  }

  public PocTemplate setGoodBlockingMissScore(Long goodBlockingMissScore) {
    this.goodBlockingMissScore = goodBlockingMissScore;
    return this;
  }

  public Long getBadBocSpeedScore() {
    return badBocSpeedScore;
  }

  public PocTemplate setBadBocSpeedScore(Long badBocSpeedScore) {
    this.badBocSpeedScore = badBocSpeedScore;
    return this;
  }

  public Long getMiddleBocSpeedScore() {
    return middleBocSpeedScore;
  }

  public PocTemplate setMiddleBocSpeedScore(Long middleBocSpeedScore) {
    this.middleBocSpeedScore = middleBocSpeedScore;
    return this;
  }

  public Long getPoorBocSpeedScore() {
    return poorBocSpeedScore;
  }

  public PocTemplate setPoorBocSpeedScore(Long poorBocSpeedScore) {
    this.poorBocSpeedScore = poorBocSpeedScore;
    return this;
  }

  public Long getFoundationFrom9900To9999() {
    return foundationFrom9900To9999;
  }

  public PocTemplate setFoundationFrom9900To9999(Long foundationFrom9900To9999) {
    this.foundationFrom9900To9999 = foundationFrom9900To9999;
    return this;
  }

  public Long getFoundationFrom9700To9900() {
    return foundationFrom9700To9900;
  }

  public PocTemplate setFoundationFrom9700To9900(Long foundationFrom9700To9900) {
    this.foundationFrom9700To9900 = foundationFrom9700To9900;
    return this;
  }

  public Long getFoundationFrom0000To9700() {
    return foundationFrom0000To9700;
  }

  public PocTemplate setFoundationFrom0000To9700(Long foundationFrom0000To9700) {
    this.foundationFrom0000To9700 = foundationFrom0000To9700;
    return this;
  }

  public Long getCommunityFrom9700To9900() {
    return communityFrom9700To9900;
  }

  public PocTemplate setCommunityFrom9700To9900(Long communityFrom9700To9900) {
    this.communityFrom9700To9900 = communityFrom9700To9900;
    return this;
  }

  public Long getCommunityFrom9000To9700() {
    return communityFrom9000To9700;
  }

  public PocTemplate setCommunityFrom9000To9700(Long communityFrom9000To9700) {
    this.communityFrom9000To9700 = communityFrom9000To9700;
    return this;
  }

  public Long getCommunityFrom0000To9000() {
    return communityFrom0000To9000;
  }

  public PocTemplate setCommunityFrom0000To9000(Long communityFrom0000To9000) {
    this.communityFrom0000To9000 = communityFrom0000To9000;
    return this;
  }

  public Long getHbFrom9900To100() {
    return hbFrom9900To100;
  }

  public PocTemplate setHbFrom9900To100(Long hbFrom9900To100) {
    this.hbFrom9900To100 = hbFrom9900To100;
    return this;
  }

  public Long getHbFrom9700To100() {
    return hbFrom9700To100;
  }

  public PocTemplate setHbFrom9700To100(Long hbFrom9700To100) {
    this.hbFrom9700To100 = hbFrom9700To100;
    return this;
  }

  public Long getHbFrom0000To9000() {
    return hbFrom0000To9000;
  }

  public PocTemplate setHbFrom0000To9000(Long hbFrom0000To9000) {
    this.hbFrom0000To9000 = hbFrom0000To9000;
    return this;
  }

  public Long getNormalFrom9700To100() {
    return normalFrom9700To100;
  }

  public PocTemplate setNormalFrom9700To100(Long normalFrom9700To100) {
    this.normalFrom9700To100 = normalFrom9700To100;
    return this;
  }

  public Long getNormalFrom9000To100() {
    return normalFrom9000To100;
  }

  public PocTemplate setNormalFrom9000To100(Long normalFrom9000To100) {
    this.normalFrom9000To100 = normalFrom9000To100;
    return this;
  }

  public Long getVersion() {
    return version;
  }

  public PocTemplate setVersion(Long version) {
    this.version = version;
    return this;
  }
}
