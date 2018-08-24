/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.conch.vm.program;

import org.conch.*;
import org.conch.vm.DataWord;
import org.conch.vm.crypto.ECKey;
import org.conch.vm.util.ByteUtil;
import org.conch.vm.util.RLP;
import org.conch.vm.util.RLPList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.conch.vm.util.ByteUtil.ZERO_BYTE_ARRAY;
import static org.conch.vm.util.ByteUtil.toHexString;

public class InternalTransaction extends TransactionImpl {

    private byte[] parentHash;
    private int deep;
    private int index;
    private boolean rejected = false;
    private String note;
    /* Tx in encoded form */
    protected byte[] rlpEncoded;
    /* a counter used to make sure each transaction can only be processed once */
    private byte[] nonce;

    public InternalTransaction(BuilderImpl builder) throws ConchException.NotValidException {
        super(builder, null);
    }

    public static BuilderImpl getBuilder(DataWord gasPrice, DataWord gasLimit, byte[] sendAddress,
                                         byte[] receiveAddress, byte[] value, byte[] data) {
        Attachment.Contract attachment = new Attachment.Contract(false, gasPrice.longValue(), gasLimit.longValue(), data);
        BuilderImpl builder = new BuilderImpl((byte) 1, sendAddress,
                ByteUtil.byteArrayToLong(value) * Constants.ONE_SS, 0, (short) 60,
                attachment)
                .timestamp(Conch.getEpochTime())
                .recipientId(ByteUtil.byteArrayToLong(receiveAddress));
        return builder;
    }

    public void setPara(byte[] parentHash, int deep, int index, String note, byte[] nonce) {
        this.parentHash = parentHash;
        this.deep = deep;
        this.index = index;
        this.note = note;
        this.nonce = nonce;
    }

    private static byte[] getData(DataWord gasPrice) {
        return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
    }

    public void reject() {
        this.rejected = true;
    }


    public int getDeep() {
        rlpParse();
        return deep;
    }

    public boolean isRejected() {
        rlpParse();
        return rejected;
    }

    public String getNote() {
        rlpParse();
        return note;
    }

    public byte[] getSender() {
        rlpParse();
        return getSender();
    }

    public byte[] getParentHash() {
        rlpParse();
        return parentHash;
    }

    public byte[] getNonce() {
        rlpParse();

        return nonce == null ? ZERO_BYTE_ARRAY : nonce;
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {

            byte[] nonce = getNonce();
            boolean isEmptyNonce = isEmpty(nonce) || (getLength(nonce) == 1 && nonce[0] == 0);

            this.rlpEncoded = RLP.encodeList(
                    RLP.encodeElement(isEmptyNonce ? null : nonce),
                    RLP.encodeElement(this.parentHash),
                    RLP.encodeElement(getSender()),
                    // TODO wj !!!!!!!!!!!!!!!address?
                    RLP.encodeElement(ByteUtil.longToBytes(getRecipientId())),
                    RLP.encodeString(this.note),
                    encodeInt(this.deep),
                    encodeInt(this.index),
                    encodeInt(this.rejected ? 1 : 0)
            );
        }

        return rlpEncoded;
    }

    public byte[] getEncodedRaw() {
        return getEncoded();
    }

    public synchronized void rlpParse() {
        RLPList decodedTxList = RLP.decode2(rlpEncoded);
        RLPList transaction = (RLPList) decodedTxList.get(0);

        this.parentHash = transaction.get(1).getRLPData();
        this.note = new String(transaction.get(8).getRLPData());
        this.deep = decodeInt(transaction.get(9).getRLPData());
        this.index = decodeInt(transaction.get(10).getRLPData());
        this.rejected = decodeInt(transaction.get(11).getRLPData()) == 1;
    }


    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    private static int bytesToInt(byte[] bytes) {
        return isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static byte[] encodeInt(int value) {
        return RLP.encodeElement(intToBytes(value));
    }

    private static int decodeInt(byte[] encoded) {
        return bytesToInt(encoded);
    }

    public ECKey getKey() {
        throw new UnsupportedOperationException("Cannot sign internal transaction.");
    }

    public void sign(byte[] privKeyBytes) throws ECKey.MissingPrivateKeyException {
        throw new UnsupportedOperationException("Cannot sign internal transaction.");
    }

    @Override
    public String toString() {
        return "TransactionData [" +
                "  parentHash=" + toHexString(getParentHash()) +
                ", nonce=" + toHexString(getNonce()) +
                ", sendAddress=" + toHexString(getSender()) +
                ", note=" + getNote() +
                ", deep=" + getDeep() +
                ", index=" + getIndex() +
                ", rejected=" + isRejected() +
                "]";
    }
}
