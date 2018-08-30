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
package org.conch.vm.program.invoke;

import org.conch.Attachment;
import org.conch.Block;
import org.conch.Transaction;
import org.conch.vm.DataWord;
import org.conch.vm.db.BlockStore;
import org.conch.vm.db.Repository;
import org.conch.vm.program.Program;
import org.conch.vm.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

/**
 * @author Roman Mandeleil
 * @since 08.06.2014
 */
public class ProgramInvokeFactoryImpl implements ProgramInvokeFactory {

    private static final Logger logger = LoggerFactory.getLogger("VM");

    // Invocation by the wire tx
    @Override
    public ProgramInvoke createProgramInvoke(Transaction tx, Block block, Repository repository,
                                             BlockStore blockStore) {

        Attachment.Contract contract = (Attachment.Contract) tx.getAttachment();
        /***         ADDRESS op       ***/
        // YP: Get address of currently executing account.
        // TODO wj Should create one new account
        byte[] address = contract.getReceiveAddress();

        /***         ORIGIN op       ***/
        // YP: This is the sender of original transaction; it is never a contract.
        byte[] origin = ByteUtil.longToBytes(tx.getSenderId());

        /***         CALLER op       ***/
        // YP: This is the address of the account that is directly responsible for this execution.
        byte[] caller = ByteUtil.longToBytes(tx.getSenderId());

        /***         BALANCE op       ***/
        byte[] balance = repository.getBalance(address).toByteArray();

        /***         GASPRICE op       ***/
        // TODO wj gas set
        byte[] gasPrice = ByteUtil.longToBytes(contract.getGasPrice());

        /*** GAS op ***/
        byte[] gas = ByteUtil.longToBytes(contract.getGasLimit());

        /***        CALLVALUE op      ***/
        byte[] callValue = nullToEmpty(ByteUtil.longToBytes(tx.getAmountNQT()));

        /***     CALLDATALOAD  op   ***/
        /***     CALLDATACOPY  op   ***/
        /***     CALLDATASIZE  op   ***/

        byte[] data = contract.isContractCreation() ? ByteUtil.EMPTY_BYTE_ARRAY : nullToEmpty(contract.getData());

        /***    PREVHASH  op  ***/
        byte[] lastHash = block.getPreviousBlockHash();

        /***   COINBASE  op ***/
        byte[] coinbase = block.getGeneratorPublicKey();

        /*** TIMESTAMP  op  ***/
        long timestamp = block.getTimestamp();

        /*** NUMBER  op  ***/
        long number = block.getHeight();

        /*** DIFFICULTY  op  ***/
        byte[] difficulty = block.getCumulativeDifficulty().toByteArray();

        // TODO wj modify the block total can execute step of contract
        /*** GASLIMIT op ***/
        byte[] gaslimit = ByteUtil.longToBytes(block.getTotalFeeNQT());

        if (logger.isInfoEnabled()) {
            logger.info("Top level call: \n" +
                            "tx.hash={}\n" +
                            "address={}\n" +
                            "origin={}\n" +
                            "caller={}\n" +
                            "balance={}\n" +
                            "gasPrice={}\n" +
                            "gas={}\n" +
                            "callValue={}\n" +
                            "data={}\n" +
                            "lastHash={}\n" +
                            "coinbase={}\n" +
                            "timestamp={}\n" +
                            "blockNumber={}\n" +
                            "difficulty={}\n" +
                            "gaslimit={}\n",

                    tx.getFullHash(),
                    ByteUtil.toHexString(address),
                    ByteUtil.toHexString(origin),
                    ByteUtil.toHexString(caller),
                    ByteUtil.bytesToBigInteger(balance),
                    ByteUtil.bytesToBigInteger(gasPrice),
                    ByteUtil.bytesToBigInteger(gas),
                    ByteUtil.bytesToBigInteger(callValue),
                    ByteUtil.toHexString(data),
                    ByteUtil.toHexString(lastHash),
                    ByteUtil.toHexString(coinbase),
                    timestamp,
                    number,
                    ByteUtil.toHexString(difficulty),
                    gaslimit);
        }

        return new ProgramInvokeImpl(address, origin, caller, balance, gasPrice, gas, callValue, data,
                lastHash, coinbase, timestamp, number, difficulty, gaslimit,
                repository, blockStore);
    }

    /**
     * This invocation created for contract call contract
     */
    @Override
    public ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                             DataWord inValue, DataWord inGas,
                                             BigInteger balanceInt, byte[] dataIn,
                                             Repository repository, BlockStore blockStore,
                                             boolean isStaticCall, boolean byTestingSuite) {

        DataWord address = toAddress;
        DataWord origin = program.getOriginAddress();
        DataWord caller = callerAddress;

        DataWord balance = new DataWord(balanceInt.toByteArray());
        DataWord gasPrice = program.getGasPrice();
        DataWord gas = inGas;
        DataWord callValue = inValue;

        byte[] data = dataIn;
        DataWord lastHash = program.getPrevHash();
        DataWord coinbase = program.getCoinbase();
        DataWord timestamp = program.getTimestamp();
        DataWord number = program.getNumber();
        DataWord difficulty = program.getDifficulty();
        DataWord gasLimit = program.getGasLimit();

        if (logger.isInfoEnabled()) {
            logger.info("Internal call: \n" +
                            "address={}\n" +
                            "origin={}\n" +
                            "caller={}\n" +
                            "balance={}\n" +
                            "gasPrice={}\n" +
                            "gas={}\n" +
                            "callValue={}\n" +
                            "data={}\n" +
                            "lastHash={}\n" +
                            "coinbase={}\n" +
                            "timestamp={}\n" +
                            "blockNumber={}\n" +
                            "difficulty={}\n" +
                            "gaslimit={}\n",
                    ByteUtil.toHexString(address.getLast20Bytes()),
                    ByteUtil.toHexString(origin.getLast20Bytes()),
                    ByteUtil.toHexString(caller.getLast20Bytes()),
                    balance.toString(),
                    gasPrice.longValue(),
                    gas.longValue(),
                    ByteUtil.toHexString(callValue.getNoLeadZeroesData()),
                    ByteUtil.toHexString(data),
                    ByteUtil.toHexString(lastHash.getData()),
                    ByteUtil.toHexString(coinbase.getLast20Bytes()),
                    timestamp.longValue(),
                    number.longValue(),
                    ByteUtil.toHexString(difficulty.getNoLeadZeroesData()),
                    gasLimit.bigIntValue());
        }

        return new ProgramInvokeImpl(address, origin, caller, balance, gasPrice, gas, callValue,
                data, lastHash, coinbase, timestamp, number, difficulty, gasLimit,
                repository, program.getCallDeep() + 1, blockStore, isStaticCall, byTestingSuite);
    }
}
