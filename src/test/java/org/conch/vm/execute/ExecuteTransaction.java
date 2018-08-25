package org.conch.vm.execute;

import org.conch.Attachment;
import org.conch.Conch;
import org.conch.Transaction;
import org.conch.crypto.Crypto;
import org.conch.util.Convert;
import org.conch.vm.BytecodeCompiler;
import org.conch.vm.db.BlockStoreDummy;
import org.conch.vm.program.Program;
import org.conch.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.conch.vm.program.invoke.ProgramInvokeMockImpl;
import org.conch.vm.util.ByteUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExecuteTransaction {
    private ProgramInvokeMockImpl invoke;
    private Program program;

    @Before
    public void setup() {
        invoke = new ProgramInvokeMockImpl();
    }

    @After
    public void tearDown() {
        invoke.getRepository().close();
    }

    @Test
    public void testTransaction() {
        try {
            String secretPhrase = "ready black twenty take pressure lot wild time themselves lead fact soul";
            byte[] data = compile("PUSH1 0x23 PUSH1 0x08 JUMP PUSH1 0x01 JUMPDEST PUSH1 0x02 SSTORE");
            byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString("87cb16b6f9bafad1f2cf77a5dfb470a0a2d2907ba67580c55457c6a72dfec471");
            Attachment.Contract contract = new Attachment.Contract(true, 1, 1, data);
            Transaction.Builder builder = Conch.newTransactionBuilder(publicKey, 100, 1,
                    Short.MAX_VALUE, contract);
            if (contract.getTransactionType().canHaveRecipient()) {
                builder.recipientId(ByteUtil.byteArrayToLong(Crypto.getPublicKey("into anything sort storm more gain flood house grade harsh know accept")));
            }
            Transaction transaction = builder.build(secretPhrase);

            TransactionExecutor executor = new TransactionExecutor
                    (transaction, publicKey, invoke.getRepository(), new BlockStoreDummy(),
                            new ProgramInvokeFactoryImpl(), Conch.getBlockchain().getLastBlock())
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private byte[] compile(String code) {
        return new BytecodeCompiler().compile(code);
    }
}
