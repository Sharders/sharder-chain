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
package org.conch.vm.execute;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.conch.Attachment;
import org.conch.Block;
import org.conch.Constants;
import org.conch.Transaction;
import org.conch.vm.*;
import org.conch.vm.crypto.HashUtil;
import org.conch.vm.db.AccountState;
import org.conch.vm.db.BlockStore;
import org.conch.vm.db.ContractDetails;
import org.conch.vm.db.Repository;
import org.conch.vm.program.Program;
import org.conch.vm.program.ProgramResult;
import org.conch.vm.program.invoke.ProgramInvoke;
import org.conch.vm.program.invoke.ProgramInvokeFactory;
import org.conch.vm.util.ByteArraySet;
import org.conch.vm.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.conch.vm.util.BIUtil.*;
import static org.conch.vm.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.conch.vm.util.ByteUtil.toHexString;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger("execute");
    private static final Logger stateLogger = LoggerFactory.getLogger("state");

    private Transaction tx;
    Attachment.Contract contract;
    private Repository track;
    private Repository cacheTrack;
    private BlockStore blockStore;
    private final long gasUsedInTheBlock;
    private boolean readyToExecute = false;
    private String execError;

    private ProgramInvokeFactory programInvokeFactory;
    private byte[] coinbase;

    private TransactionReceipt receipt;
    private ProgramResult result = new ProgramResult();
    private Block currentBlock;

    private VM vm;
    private Program program;

    PrecompiledContracts.PrecompiledContract precompiledContract;

    BigInteger m_endGas = BigInteger.ZERO;
    long basicTxCost = 0;
    List<LogInfo> logs = null;

    private ByteArraySet touchedAccounts = new ByteArraySet();

    boolean localCall = false;

    private static GasCost gasCost = new GasCost();

    public TransactionExecutor(Transaction tx, byte[] coinbase, Repository track, BlockStore blockStore,
                               ProgramInvokeFactory programInvokeFactory, Block currentBlock) {

        this(tx, coinbase, track, blockStore, programInvokeFactory, currentBlock, 0);
    }

    public TransactionExecutor(Transaction tx, byte[] coinbase, Repository track, BlockStore blockStore,
                               ProgramInvokeFactory programInvokeFactory, Block currentBlock, long gasUsedInTheBlock) {

        this.tx = tx;
        this.contract = (Attachment.Contract) tx.getAttachment();
        this.coinbase = coinbase;
        this.track = track;
        this.cacheTrack = track.startTracking();
        this.blockStore = blockStore;
        this.programInvokeFactory = programInvokeFactory;
        this.currentBlock = currentBlock;
        this.gasUsedInTheBlock = gasUsedInTheBlock;
        this.m_endGas = toBI(contract.getGasLimit());
    }

    private void execError(String err) {
        logger.warn(err);
        execError = err;
    }

    /**
     * Do all the basic validation, if the executor
     * will be ready to run the transaction at the end
     * set readyToExecute = true
     */
    public void init() {
        long nonZeroes = contract.getData().length;
        long zeroVals = ArrayUtils.getLength(contract.getData()) - nonZeroes;

        // TODO wj calculate from upper
        basicTxCost = gasCost.getTRANSACTION() + zeroVals * gasCost.getTX_ZERO_DATA() +
                nonZeroes * gasCost.getTX_NO_ZERO_DATA();

        if (localCall) {
            readyToExecute = true;
            return;
        }

        BigInteger txGasLimit = new BigInteger(1, ByteUtil.longToBytes(contract.getGasLimit()));
        //TODO wj block gas limit ?? not used Long.MAX_VALUE / 2
        byte[] blockGasLimit = ByteUtil.longToBytes(Long.MAX_VALUE / 2);
        BigInteger curBlockGasLimit = new BigInteger(1, blockGasLimit);

        boolean cumulativeGasReached = txGasLimit.add(BigInteger.valueOf(gasUsedInTheBlock)).compareTo(curBlockGasLimit) > 0;
        if (cumulativeGasReached) {

            execError(String.format("Too much gas used in this block: Require: %s Got: %s", new BigInteger(1, blockGasLimit).longValue() - toBI(contract.getGasLimit()).longValue(), toBI(contract.getGasLimit()).longValue()));

            return;
        }

        if (txGasLimit.compareTo(BigInteger.valueOf(basicTxCost)) < 0) {

            execError(String.format("Not enough gas for transaction execution: Require: %s Got: %s", basicTxCost, txGasLimit));

            return;
        }

        BigInteger reqNonce = track.getNonce(tx.getSenderPublicKey());
        // TODO wj txNounce is not defined
        BigInteger txNonce = reqNonce;
        if (isNotEqual(reqNonce, txNonce)) {
            execError(String.format("Invalid nonce: required: %s , tx.nonce: %s", reqNonce, txNonce));

            return;
        }

        BigInteger txGasCost = toBI(contract.getGasPrice()).multiply(txGasLimit);
        BigInteger totalCost = toBI(tx.getAmountNQT()).add(txGasCost);
        BigInteger senderBalance = track.getBalance(tx.getSenderPublicKey());

        if (!isCovers(senderBalance, totalCost)) {

            execError(String.format("Not enough cash: Require: %s, Sender cash: %s", totalCost, senderBalance));

            return;
        }

        readyToExecute = true;
    }

    public void execute() {

        if (!readyToExecute) return;

        if (!localCall) {
            track.increaseNonce(tx.getSenderPublicKey());

            BigInteger txGasLimit = toBI(contract.getGasLimit());
            BigInteger txGasCost = toBI(contract.getGasPrice()).multiply(txGasLimit);
            track.addBalance(tx.getSenderPublicKey(), txGasCost.negate());

            if (logger.isInfoEnabled())
                logger.info("Paying: txGasCost: [{}], gasPrice: [{}], gasLimit: [{}]", txGasCost, toBI(contract.getGasPrice()), txGasLimit);
        }

        if (contract.isContractCreation()) {
            create();
        } else {
            call();
        }
    }

    private void call() {
        if (!readyToExecute) return;

        byte[] targetAddress = ByteUtil.longToBytes(tx.getRecipientId());
        precompiledContract = PrecompiledContracts.getContractForAddress(new DataWord(targetAddress));

        if (precompiledContract != null) {
            long requiredGas = precompiledContract.getGasForData(contract.getData());

            BigInteger spendingGas = BigInteger.valueOf(requiredGas).add(BigInteger.valueOf(basicTxCost));

            if (!localCall && m_endGas.compareTo(spendingGas) < 0) {
                // no refund
                // no endowment
                execError("Out of Gas calling precompiled contract 0x" + toHexString(targetAddress) +
                        ", required: " + spendingGas + ", left: " + m_endGas);
                m_endGas = BigInteger.ZERO;
                return;
            } else {

                m_endGas = m_endGas.subtract(spendingGas);

                // FIXME: save return for vm trace
                Pair<Boolean, byte[]> out = precompiledContract.execute(contract.getData());

                if (!out.getLeft()) {
                    execError("Error executing precompiled contract 0x" + toHexString(targetAddress));
                    m_endGas = BigInteger.ZERO;
                    return;
                }
            }

        } else {

            byte[] code = track.getCode(targetAddress);
            if (isEmpty(code)) {
                m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost));
                result.spendGas(basicTxCost);
            } else {
                ProgramInvoke programInvoke =
                        programInvokeFactory.createProgramInvoke(tx, currentBlock, cacheTrack, blockStore);

                this.vm = new VM();
                this.program = new Program(track.getCodeHash(targetAddress), code, programInvoke, tx);
            }
        }

        BigInteger endowment = toBI(tx.getAmountNQT());
        transfer(cacheTrack, tx.getSenderPublicKey(), targetAddress, endowment);

        touchedAccounts.add(targetAddress);
    }

    private void create() {
        byte[] newContractAddress = null;
        if (contract.isContractCreation()) {
            // TODO wj address create by nonce or new Sharder account
            newContractAddress = HashUtil.calcNewAddr(tx.getSenderPublicKey(), ByteUtil.longToBytes(1));
        }

        AccountState existingAddr = cacheTrack.getAccountState(newContractAddress);
        if (existingAddr != null && existingAddr.isContractExist()) {
            execError("Trying to create a contract with existing contract address: 0x" + toHexString(newContractAddress));
            m_endGas = BigInteger.ZERO;
            return;
        }

        //In case of hashing collisions (for TCK tests only), check for any balance before createAccount()
        BigInteger oldBalance = track.getBalance(newContractAddress);
        cacheTrack.createAccount(newContractAddress);
        cacheTrack.addBalance(newContractAddress, oldBalance);
        cacheTrack.increaseNonce(newContractAddress);

        if (isEmpty(contract.getData())) {
            m_endGas = m_endGas.subtract(BigInteger.valueOf(basicTxCost));
            result.spendGas(basicTxCost);
        } else {
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(tx, currentBlock, cacheTrack, blockStore);

            this.vm = new VM();
            this.program = new Program(contract.getData(), programInvoke, tx);

            // reset storage if the contract with the same address already exists
            // TCK test case only - normally this is near-impossible situation in the real network
            // TODO make via Trie.clear() without keyset
//            ContractDetails contractDetails = program.getStorage().getContractDetails(newContractAddress);
//            for (DataWord key : contractDetails.getStorageKeys()) {
//                program.storageSave(key, DataWord.ZERO);
//            }
        }

        BigInteger endowment = toBI(tx.getAmountNQT());
        transfer(cacheTrack, tx.getSenderPublicKey(), newContractAddress, endowment);

        touchedAccounts.add(newContractAddress);
    }

    public void go() {
        if (!readyToExecute) return;

        try {

            if (vm != null) {

                // Charge basic cost of the transaction
                program.spendGas(basicTxCost, "TRANSACTION COST");

                // TODO wj vm execute always
                vm.play(program);

                result = program.getResult();
                m_endGas = toBI(contract.getGasLimit()).subtract(toBI(program.getResult().getGasUsed()));

                if (contract.isContractCreation() && !result.isRevert()) {
                    int returnDataGasValue = getLength(program.getResult().getHReturn()) *
                            gasCost.getCREATE_DATA();
                    if (getLength(result.getHReturn()) > Constants.MAX_CONTRACT_SZIE) {
                        // Contract size too large
                        program.setRuntimeFailure(Program.Exception.notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
                                returnDataGasValue, program));
                        result = program.getResult();
                        result.setHReturn(EMPTY_BYTE_ARRAY);
                    } else {
                        // Contract successfully created
                        m_endGas = m_endGas.subtract(BigInteger.valueOf(returnDataGasValue));
                        byte[] newContractAddress = null;
                        if (contract.isContractCreation()) {
                            // TODO wj address create by nonce or new Sharder account
                            newContractAddress = HashUtil.calcNewAddr(tx.getSenderPublicKey(), ByteUtil.longToBytes(1));
                        }
                        cacheTrack.saveCode(newContractAddress, result.getHReturn());
                    }
                }

                if (result.getException() != null || result.isRevert()) {
                    result.getDeleteAccounts().clear();
                    result.getLogInfoList().clear();
                    result.resetFutureRefund();
                    rollback();

                    if (result.getException() != null) {
                        throw result.getException();
                    } else {
                        execError("REVERT opcode executed");
                    }
                } else {
                    touchedAccounts.addAll(result.getTouchedAccounts());
                    cacheTrack.commit();
                }

            } else {
                cacheTrack.commit();
            }

        } catch (Throwable e) {

            // TODO: catch whatever they will throw on you !!!
//            https://github.com/ethereum/cpp-ethereum/blob/develop/libethereum/Executive.cpp#L241
            rollback();
            m_endGas = BigInteger.ZERO;
            execError(e.getMessage());
        }
    }

    private void rollback() {

        cacheTrack.rollback();

        byte[] newContractAddress = null;
        if (contract.isContractCreation()) {
            // TODO wj address create by nonce or new Sharder account
            newContractAddress = HashUtil.calcNewAddr(tx.getSenderPublicKey(), ByteUtil.longToBytes(1));
        }
        // remove touched account
        touchedAccounts.remove(
                contract.isContractCreation() ? newContractAddress : ByteUtil.longToBytes(tx.getRecipientId()));
    }

    public TransactionExecutionSummary finalization() {
        if (!readyToExecute) return null;

        TransactionExecutionSummary.Builder summaryBuilder = TransactionExecutionSummary.builderFor(tx)
                .gasLeftover(m_endGas)
                .logs(result.getLogInfoList())
                .result(result.getHReturn());

        if (result != null) {
            // Accumulate refunds for suicides
            result.addFutureRefund(result.getDeleteAccounts().size() * gasCost.getSUICIDE_REFUND());
            long gasRefund = Math.min(result.getFutureRefund(), getGasUsed() / 2);
            byte[] newContractAddress = null;
            if (contract.isContractCreation()) {
                // TODO wj address create by nonce or new Sharder account
                newContractAddress = HashUtil.calcNewAddr(tx.getSenderPublicKey(), ByteUtil.longToBytes(1));
            }
            byte[] addr = contract.isContractCreation() ? newContractAddress : ByteUtil.longToBytes(tx.getRecipientId());
            m_endGas = m_endGas.add(BigInteger.valueOf(gasRefund));

            summaryBuilder
                    .gasUsed(toBI(result.getGasUsed()))
                    .gasRefund(toBI(gasRefund))
                    .deletedAccounts(result.getDeleteAccounts())
                    .internalTransactions(result.getInternalTransactions());

            ContractDetails contractDetails = track.getContractDetails(addr);
            if (contractDetails != null) {
                // TODO
//                summaryBuilder.storageDiff(track.getContractDetails(addr).getStorage());
//
//                if (program != null) {
//                    summaryBuilder.touchedStorage(contractDetails.getStorage(), program.getStorageDiff());
//                }
            }

            if (result.getException() != null) {
                summaryBuilder.markAsFailed();
            }
        }

        TransactionExecutionSummary summary = summaryBuilder.build();

        // Refund for gas leftover
        track.addBalance(tx.getSenderPublicKey(), summary.getLeftover().add(summary.getRefund()));
        logger.info("Pay total refund to sender: [{}], refund val: [{}]", toHexString(tx.getSenderPublicKey()), summary.getRefund());

        // Transfer fees to miner
        track.addBalance(coinbase, summary.getFee());
        touchedAccounts.add(coinbase);
        logger.info("Pay fees to miner: [{}], feesEarned: [{}]", toHexString(coinbase), summary.getFee());

        if (result != null) {
            logs = result.getLogInfoList();
            // Traverse list of suicides
            for (DataWord address : result.getDeleteAccounts()) {
                track.delete(address.getLast20Bytes());
            }
        }

        for (byte[] acctAddr : touchedAccounts) {
            AccountState state = track.getAccountState(acctAddr);
            if (state != null && state.isEmpty()) {
                track.delete(acctAddr);
            }
        }
        return summary;
    }

    public TransactionExecutor setLocalCall(boolean localCall) {
        this.localCall = localCall;
        return this;
    }


    public TransactionReceipt getReceipt() {
        if (receipt == null) {
            receipt = new TransactionReceipt();
            long totalGasUsed = gasUsedInTheBlock + getGasUsed();
            receipt.setCumulativeGas(totalGasUsed);
            receipt.setTransaction(tx);
            receipt.setLogInfoList(getVMLogs());
            receipt.setGasUsed(getGasUsed());
            receipt.setExecutionResult(getResult().getHReturn());
            receipt.setError(execError);
//            receipt.setPostTxState(track.getRoot()); // TODO later when RepositoryTrack.getRoot() is implemented
        }
        return receipt;
    }

    public List<LogInfo> getVMLogs() {
        return logs;
    }

    public ProgramResult getResult() {
        return result;
    }

    public long getGasUsed() {
        return toBI(contract.getGasLimit()).subtract(m_endGas).longValue();
    }

}
