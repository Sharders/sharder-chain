package org.conch.peer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.util.Convert;
import org.conch.util.IpUtil;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Certified peer include bound account and basic peer info:
 *
 * UPDATE#1:
 * process PocNodeConf tx and update certified peer list.
 * org.conch.consensus.poc.PocProcessorImpl#nodeTypeTxProcess(int, org.conch.consensus.poc.tx.PocTxBody.PocNodeType)
 *
 * UPDATE#2: get hub bind details form mwfs.io and update certified peer list.
 * org.conch.peer.Peers#GET_HUB_PEER_THREAD
 *
 * UPDATE#3:
 * syn peers and update certified peer list.
 * org.conch.consensus.poc.PocProcessorImpl#peerSynThread
 *
 * @author <a href="mailto:xy@mwfs.io">Ben</a>
 * @since 2019-03-29
 */
public class CertifiedPeer implements Serializable {
    int height = -1;
    int endHeight = -2;
    Peer.Type type;
    //peerHost is public ip or announcedAddress(NatIp+Port)
    String host;
    // real public ip
    String ip;
    boolean useNat = false;
    long boundAccountId;
    String boundRS;
    Timestamp updateTime;


    public CertifiedPeer(Peer.Type type, String host, long accountId, long lastUpdateMS) {
        this.type = type != null ? type : Peer.Type.NORMAL;
        this.useNat = Peers.isUseNATService(host);
        this.host = host;
        this.boundAccountId = accountId;
        this.boundRS = Account.rsAccount(accountId);
        try {
            this.ip = IpUtil.getIpFromUrl(host);
        } catch (Exception ignore) {
            //ignore
        }
        updateTimeSet(lastUpdateMS);
    }

    public CertifiedPeer(Peer.Type type, String host, long accountId) {
        this(type, host, accountId, System.currentTimeMillis());
    }

    public CertifiedPeer(int height, Peer.Type type, String host, long accountId) {
        this(type, host, accountId);
        this.height = height;
    }

    private void updateTimeSet(long timeMS) {
        try {
            this.updateTime = new Timestamp(timeMS);
        } catch (Exception ignore) {
            //ignore
        }
    }

    private void boundAccountSet(long accountId) {
        this.boundAccountId = accountId;
        this.boundRS = Account.rsAccount(accountId);
    }

    public CertifiedPeer update(int height) {
        if (this.height < height) this.height = height;
        updateTimeSet(System.currentTimeMillis());
        return this;
    }

    public CertifiedPeer update(long accountId) {
        boundAccountSet(accountId);
        updateTimeSet(System.currentTimeMillis());
        return this;
    }

    public CertifiedPeer update(Peer.Type type) {
        this.type = type;
        updateTimeSet(System.currentTimeMillis());
        return this;
    }

    public boolean isSame(String peerHost) {
        String peerIp = IpUtil.getIpFromUrl(peerHost);
        if (useNat) {
            return host.equalsIgnoreCase(peerHost);
        } else {
            return ip.equalsIgnoreCase(peerIp);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof CertifiedPeer) {
            CertifiedPeer cp = (CertifiedPeer) obj;
            if (useNat) {
                return host.equalsIgnoreCase(cp.host);
            } else {
                return ip.equalsIgnoreCase(cp.ip);
            }
        }
        return false;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isType(Peer.Type type){
        return (type == null || this.type == null)  ? false : this.type.equals(type);
    }

    public Peer.Type getType() {
        return type;
    }

    public int getTypeCode() {
        return this.type.getCode();
    }

    public void setType(Peer.Type type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isUseNat() {
        return useNat;
    }

    public long getBoundAccountId() {
        return boundAccountId;
    }

    public String getBoundRS() {
        return boundRS;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public int getUpdateTimeInEpochFormat() {
        return Convert.toEpochTime(this.updateTime.getTime());
    }

    public int getEndHeight() {
        return endHeight;
    }

    public boolean isEnd(){
        return this.endHeight >= 0;
    }

    public void end(int endHeight) throws ConchException.NotValidException {
        if(endHeight <= -1) throw new ConchException.NotValidException("certified peer is invalid: end height <= -1");
        if(endHeight < height) throw new ConchException.NotValidException("certified peer is invalid: end height " + endHeight + " < start height " + height);
        this.endHeight = endHeight;
    }

    public void check() throws ConchException.NotValidException {
        if(type == null) throw new ConchException.NotValidException("certified peer is invalid: type is null");
        if(StringUtils.isEmpty(host)) throw new ConchException.NotValidException("certified peer is invalid: host is null");
        if(boundAccountId == -1) throw new ConchException.NotValidException("certified peer is invalid: bound account id is -1");
        if(height < 0) throw new ConchException.NotValidException("certified peer is invalid: height is smaller than 0");

        // peer type check: foundation type should check the domain whether valid
        if(Peer.Type.FOUNDATION == type && !IpUtil.isFoundationDomain(host)) {
            throw new ConchException.NotValidException("certified peer is invalid: type is FOUNDATION, but hots is not valid foundation domain");
        }
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
