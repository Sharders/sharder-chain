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

package org.conch.tx;

import org.conch.chain.Block;
import org.conch.common.ConchException;
import org.conch.util.Filter;
import org.json.simple.JSONObject;

import java.util.List;

public interface Transaction {

    interface Builder {

        Builder recipientId(long recipientId);

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder appendix(Appendix.Message message);

        Builder appendix(Appendix.EncryptedMessage encryptedMessage);

        Builder appendix(Appendix.EncryptToSelfMessage encryptToSelfMessage);

        Builder appendix(Appendix.PublicKeyAnnouncement publicKeyAnnouncement);

        Builder appendix(Appendix.PrunablePlainMessage prunablePlainMessage);

        Builder appendix(Appendix.PrunableEncryptedMessage prunableEncryptedMessage);

        Builder appendix(Appendix.Phasing phasing);

        Builder timestamp(int timestamp);

        Builder ecBlockHeight(int height);

        Builder ecBlockId(long blockId);

        Transaction build() throws ConchException.NotValidException;

        Transaction build(String secretPhrase) throws ConchException.NotValidException;

    }

    long getId();

    String getStringId();

    long getSenderId();

    byte[] getSenderPublicKey();

    long getRecipientId();

    int getHeight();

    long getBlockId();

    Block getBlock();

    short getIndex();

    int getTimestamp();

    int getBlockTimestamp();

    short getDeadline();

    int getExpiration();

    long getAmountNQT();

    long getFeeNQT();

    String getReferencedTransactionFullHash();

    byte[] getSignature();

    String getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    boolean verifySignature();

    void validate() throws ConchException.ValidationException;

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    JSONObject getPrunableAttachmentJSON();

    byte getVersion();

    int getFullSize();

    Appendix.Message getMessage();

    Appendix.EncryptedMessage getEncryptedMessage();

    Appendix.EncryptToSelfMessage getEncryptToSelfMessage();

    Appendix.Phasing getPhasing();

    Appendix.PrunablePlainMessage getPrunablePlainMessage();

    Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage();

    List<? extends Appendix> getAppendages();

    List<? extends Appendix> getAppendages(boolean includeExpiredPrunable);

    List<? extends Appendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable);

    int getECBlockHeight();

    long getECBlockId();
}
