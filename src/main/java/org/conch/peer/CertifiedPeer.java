package org.conch.peer;

import org.conch.account.Account;
import org.conch.util.IpUtil;

import java.sql.Timestamp;

/**
 * Certified peer include bound account and basic peer info
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
            this.updateTime = new Timestamp(System.currentTimeMillis());
        } catch (Exception ignore) {
            //ignore
        }
    }

    public void checkOrUpdateBoundAccount() {

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
}
