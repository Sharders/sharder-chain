package org.conch.vm.db;

import org.conch.vm.DataWord;
import org.conch.vm.util.RLP;
import org.conch.vm.util.RLPList;

public class Storage {
    private byte[] rlpEncoded;
    DataWord key;
    DataWord value;

    public Storage(DataWord key, DataWord value) {
        this.key = key;
        this.value = value;
    }

    public Storage(byte[] rlpData) {
        this.rlpEncoded = rlpData;
        RLPList items = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.key = new DataWord(items.get(0).getRLPData());
        this.value = new DataWord(items.get(1).getRLPData());
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[] key = RLP.encodeElement(this.key.getData());
            byte[] value = RLP.encodeElement(this.value.getData());
            this.rlpEncoded = RLP.encodeList(key, value);
        }
        return rlpEncoded;
    }
}
