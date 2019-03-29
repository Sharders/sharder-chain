package org.conch.peer;

import org.conch.account.Account;
import org.conch.util.IpUtil;

import java.sql.Timestamp;

/**
 * Certified peer include bound account and basic peer info:
 *
 * UPDATE#1: 
 * process PocNodeConf tx and update certified peer list.
 * org.conch.consensus.poc.PocProcessorImpl#nodeTypeTxProcess(int, org.conch.consensus.poc.tx.PocTxBody.PocNodeType)
 *
 * UPDATE#2: get hub bind details form sharder.org and update certified peer list.
 * org.conch.peer.Peers#GET_HUB_PEER_THREAD
 *
 * UPDATE#3: 
 * syn peers and update certified peer list.
 * org.conch.consensus.poc.PocProcessorImpl#peerSynThread
 *
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-03-29
 */
public class CertifiedPeer {
    int height = -1;
    Peer.Type type;
    //peerHost is public ip or announcedAddress(NatIp+Port) 
    String host;
    // real public ip
    String ip;
    boolean useNat = false;
    long boundAccountId;
    String boundRS;
    Timestamp updateTime;

    public CertifiedPeer(Peer.Type type, String host, long accountId) {
        this.type = type;
        this.useNat = Peers.isUseNATService(host);
        this.host = host;
        this.boundAccountId = accountId;
        this.boundRS = Account.rsAccount(accountId);
        try {
            this.ip = IpUtil.checkOrToIp(host);
        } catch (Exception ignore) {
            //ignore
        }
        updateTimeSet();
    }

    public CertifiedPeer(int height, Peer.Type type, String host, long accountId) {
        this(type, host, accountId);
        this.height = height;
    }

    public CertifiedPeer(int height, Peer peer, long accountId) {
        this(peer.getType(), peer.getHost(), accountId);
        this.height = height;
    }

    private void updateTimeSet() {
        try {
            this.updateTime = new Timestamp(System.currentTimeMillis());
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
        updateTimeSet();
        return this;
    }

    public CertifiedPeer update(long accountId) {
        boundAccountSet(accountId);
        updateTimeSet();
        return this;
    }

    public CertifiedPeer update(Peer.Type type) {
        this.type = type;
        updateTimeSet();
        return this;
    }

    public boolean isSame(String peerHost) {
        String peerIp = IpUtil.checkOrToIp(peerHost);
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

    public Peer.Type getType() {
        return type;
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
}
