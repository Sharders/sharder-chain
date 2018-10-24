package org.conch.vm.db;

import org.conch.vm.util.ByteUtil;

public class SourceImpl implements SourceI<byte[], byte[]> {

    @Override
    public void put(byte[] key, byte[] val) {
        ContractTable contractTable = new ContractTable(ByteUtil.toHexString(key), ByteUtil.toHexString(val));
        contractTable.save();
    }

    @Override
    public byte[] get(byte[] key) {
        return ContractTable.getValueByKey(ByteUtil.toHexString(key));
    }

    @Override
    public void delete(byte[] key) {
        ContractTable contractTable = ContractTable.getInstance(ByteUtil.toHexString(key));
        contractTable.delete();
    }

    @Override
    public boolean flush() {
        return false;
    }
}
