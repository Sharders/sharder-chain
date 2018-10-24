package org.conch.vm.db;

import org.conch.Account;
import org.conch.AccountLedger;
import org.conch.vm.DataWord;
import org.conch.vm.trie.SecureTrie;
import org.conch.vm.trie.Trie;
import org.conch.vm.util.ByteUtil;
import org.conch.vm.util.RLP;

import java.sql.SQLException;
import java.util.HashMap;

public class SourceAdapter {

    public final static Serializer<DataWord, byte[]> StorageValueSerializer = new Serializer<DataWord, byte[]>() {
        @Override
        public byte[] serialize(DataWord object) {
            return RLP.encodeElement(object.getNoLeadZeroesData());
        }

        @Override
        public DataWord deserialize(byte[] stream) {
            if (stream == null || stream.length == 0) return null;
            byte[] dataDecoded = RLP.decode2(stream).get(0).getRLPData();
            return new DataWord(dataDecoded);
        }
    };


    public void flushStorage(RepositoryImpl repository, HashMap<String, HashMap<DataWord, DataWord>> storageCache) throws SQLException {
        for (String key : storageCache.keySet()) {
            AccountState accountState = repository.getAccountState(ByteUtil.hexStringToBytes(key));
            Trie<byte[]> trie = new SecureTrie(accountState.getStateRoot());
            Serializer<byte[], byte[]> keyCompositor = new NodeKeyCompositor(ByteUtil.hexStringToBytes(key));
            for (DataWord dataKey : storageCache.get(key).keySet()) {
                trie.put(keyCompositor.serialize(dataKey.getData()), StorageValueSerializer.serialize(storageCache.get(key).get(dataKey)));
            }
            trie.flush();
            repository.setRootHash(ByteUtil.hexStringToBytes(key), trie.getRootHash());
        }
    }

    public void flushCodeCache(HashMap<String, byte[]> codeCache) throws SQLException {
        for (String key : codeCache.keySet()) {
            saveValue(key, ByteUtil.toHexString(codeCache.get(key)));
        }
    }

    public void flushAccountState(Trie<byte[]> stateTrie, HashMap<String, AccountState> accountStateCache) throws SQLException {
        for (String key : accountStateCache.keySet()) {
            stateTrie.put(ByteUtil.hexStringToBytes(key), accountStateCache.get(key).getEncoded());

            // update to sharder account table
            Account account = Account.getAccount(ByteUtil.hexStringToBytes(key));
            account.addToBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.CONTRACT, accountStateCache.get(key).getNonce().intValue(),
                    accountStateCache.get(key).getBalance().longValue() - account.getBalanceNQT());
        }
        setStateRoot(stateTrie.getRootHash());
    }

    public void flushAccountStateToDB(Trie<byte[]> stateTrie, HashMap<String, AccountState> accountStateCache) throws SQLException {
        for (String key : accountStateCache.keySet()) {
            stateTrie.put(ByteUtil.hexStringToBytes(key), accountStateCache.get(key).getEncoded());
        }
    }

    private void saveValue(String key, String value) throws SQLException {
        ContractTable contractTable = new ContractTable(key, value);
        contractTable.save();
    }

    public static DataWord getStorage(Repository repository, byte[] addr, DataWord key) {
        Serializer<byte[], byte[]> keyCompositor = new NodeKeyCompositor(addr);
        AccountState accountState = repository.getAccountState(addr);
        Trie<byte[]> trie = new SecureTrie(accountState.getStateRoot());
        byte[] value = trie.get(keyCompositor.serialize(key.getData()));
        return StorageValueSerializer.deserialize(value);
    }

    public static AccountState getAccountState(Trie<byte[]> stateTrie, byte[] addr) {
        byte[] data = stateTrie.get(addr);
        if (data == null)
            return null;
        return new AccountState(stateTrie.get(addr));
    }

    public static byte[] getBytesValue(String addr) {
        return ContractTable.getValueByKey(addr);
    }

    public static byte[] getStateRoot() {
        return ContractTable.getValueByKey("stateRoot");
    }

    public static void setStateRoot(byte[] root) {
        ContractTable contractTable = new ContractTable("stateRoot", ByteUtil.toHexString(root));
        contractTable.save();
    }
}
