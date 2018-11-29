package org.conch.consensus.poc;

import org.conch.account.Account;

import java.math.BigInteger;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {
    public static PocProcessorImpl instance = getOrCreate();

    private PocProcessorImpl(){}

    private static synchronized PocProcessorImpl getOrCreate(){
        if(instance != null) return instance;

        return new PocProcessorImpl();
    }

    @Override
    public BigInteger calPocScore(Account account,int height) {

        return BigInteger.ZERO;
    }



}
