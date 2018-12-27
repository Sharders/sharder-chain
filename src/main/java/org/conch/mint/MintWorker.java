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

package org.conch.mint;

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.crypto.Crypto;
import org.conch.crypto.HashFunction;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.TrustAllSSLProvider;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MintWorker {

    public static void main(String[] args) {
        MintWorker mintWorker = new MintWorker();
        mintWorker.mint();
    }

    private void mint() {
        String currencyCode = Convert.emptyToNull(Conch.getStringProperty("sharder.mint.currencyCode"));
        if (currencyCode == null) {
            throw new IllegalArgumentException("sharder.mint.currencyCode not specified");
        }
        String secretPhrase = Convert.emptyToNull(Conch.getStringProperty("sharder.mint.secretPhrase", null, true));
        if (secretPhrase == null) {
            throw new IllegalArgumentException("sharder.mint.secretPhrase not specified");
        }
        boolean isSubmitted = Conch.getBooleanProperty("sharder.mint.isSubmitted");
        boolean isStopOnError = Conch.getBooleanProperty("sharder.mint.stopOnError");
        byte[] publicKeyHash = Crypto.sha256().digest(Crypto.getPublicKey(secretPhrase));
        long accountId = Convert.fullHashToId(publicKeyHash);
        String rsAccount = Convert.rsAccount(accountId);
        JSONObject currency = getCurrency(currencyCode);
        if (currency.get("currency") == null) {
            throw new IllegalArgumentException("Invalid currency code " + currencyCode);
        }
        long currencyId = Convert.parseUnsignedLong((String) currency.get("currency"));
        if (currency.get("algorithm") == null) {
            throw new IllegalArgumentException("Minting algorithm not specified, currency " + currencyCode + " is not mintable");
        }
        byte algorithm = (byte)(long) currency.get("algorithm");
        byte decimal = (byte)(long) currency.get("decimals");
        String unitsStr = Conch.getStringProperty("sharder.mint.unitsPerMint");
        double wholeUnits = 1;
        if (unitsStr != null && unitsStr.length() > 0) {
            wholeUnits = Double.parseDouble(unitsStr);
        }
        long units = (long)(wholeUnits * Math.pow(10, decimal));
        JSONObject mintingTarget = getMintingTarget(currencyId, rsAccount, units);
        long counter = (long) mintingTarget.get("counter");
        byte[] target = Convert.parseHexString((String) mintingTarget.get("targetBytes"));
        BigInteger difficulty = new BigInteger((String)mintingTarget.get("difficulty"));
        long initialNonce = Conch.getIntProperty("sharder.mint.initialNonce");
        if (initialNonce == 0) {
            initialNonce = new Random().nextLong();
        }
        int threadPoolSize = Conch.getIntProperty("sharder.mint.threadPoolSize");
        if (threadPoolSize == 0) {
            threadPoolSize = Runtime.getRuntime().availableProcessors();
            Logger.logDebugMessage("Thread pool size " + threadPoolSize);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        Logger.logInfoMessage("Mint worker started");
        while (true) {
            counter++;
            try {
                JSONObject response = mintImpl(secretPhrase, accountId, units, currencyId, algorithm, counter, target,
                    initialNonce, threadPoolSize, executorService, difficulty, isSubmitted);
                Logger.logInfoMessage("currency mint response:" + response.toJSONString());
            } catch (Exception e) {
                Logger.logInfoMessage("mint error", e);
                if (isStopOnError) {
                    Logger.logInfoMessage("stopping on error");
                    break;
                } else {
                    Logger.logInfoMessage("continue");
                }
            }
            mintingTarget = getMintingTarget(currencyId, rsAccount, units);
            target = Convert.parseHexString((String) mintingTarget.get("targetBytes"));
            difficulty = new BigInteger((String)mintingTarget.get("difficulty"));
        }
    }

    private JSONObject mintImpl(String secretPhrase, long accountId, long units, long currencyId, byte algorithm,
                                long counter, byte[] target, long initialNonce, int threadPoolSize, ExecutorService executorService, BigInteger difficulty, boolean isSubmitted) {
        long startTime = System.currentTimeMillis();
        List<Callable<Long>> workersList = new ArrayList<>();
        for (int i=0; i < threadPoolSize; i++) {
            HashSolver hashSolver = new HashSolver(algorithm, currencyId, accountId, counter, units, initialNonce + i, target, threadPoolSize);
            workersList.add(hashSolver);
        }
        long solution = solve(executorService, workersList);
        long computationTime = System.currentTimeMillis() - startTime;
        if (computationTime == 0) {
            computationTime = 1;
        }
        long hashes = solution - initialNonce;
        float hashesPerDifficulty = BigInteger.valueOf(-1).equals(difficulty) ? 0 : (float) hashes / difficulty.floatValue();
        Logger.logInfoMessage("solution nonce %d unitsNQT %d counter %d computed hashes %d time [sec] %.2f hash rate [KH/Sec] %d actual time vs. expected %.2f is submitted %b",
                solution, units, counter, hashes, (float) computationTime / 1000, hashes / computationTime, hashesPerDifficulty, isSubmitted);
        JSONObject response;
        if (isSubmitted) {
            response = currencyMint(secretPhrase, currencyId, solution, units, counter);
        } else {
            response = new JSONObject();
            response.put("message", "sharder.mint.isSubmitted=false therefore currency mint transaction is not submitted");
        }
        return response;
    }

    private long solve(Executor executor, Collection<Callable<Long>> solvers) {
        CompletionService<Long> ecs = new ExecutorCompletionService<>(executor);
        List<Future<Long>> futures = new ArrayList<>(solvers.size());
        solvers.forEach(solver -> futures.add(ecs.submit(solver)));
        try {
            return ecs.take().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            for (Future<Long> f : futures) {
                f.cancel(true);
            }
        }
    }

    private JSONObject currencyMint(String secretPhrase, long currencyId, long nonce, long units, long counter) {
        JSONObject ecBlock = getECBlock();
        Attachment attachment = new Attachment.MonetarySystemCurrencyMinting(nonce, currencyId, units, counter);
        Transaction.Builder builder = Conch.newTransactionBuilder(Crypto.getPublicKey(secretPhrase), 0, Constants.ONE_SS,
                (short) 120, attachment)
                .timestamp(((Long) ecBlock.get("timestamp")).intValue())
                .ecBlockHeight(((Long) ecBlock.get("ecBlockHeight")).intValue())
                .ecBlockId(Convert.parseUnsignedLong((String) ecBlock.get("ecBlockId")));
        try {
            Transaction transaction = builder.build(secretPhrase);
            Map<String, String> params = new HashMap<>();
            params.put("requestType", "broadcastTransaction");
            params.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            return getJsonResponse(params);
        } catch (ConchException.NotValidException e) {
            Logger.logInfoMessage("local signing failed", e);
            JSONObject response = new JSONObject();
            response.put("error", e.toString());
            return response;
        }
    }

    private JSONObject getCurrency(String currencyCode) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getCurrency");
        params.put("code", currencyCode);
        return getJsonResponse(params);
    }

    private JSONObject getMintingTarget(long currencyId, String rsAccount, long units) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getMintingTarget");
        params.put("currency", Long.toUnsignedString(currencyId));
        params.put("account", rsAccount);
        params.put("units", Long.toString(units));
        return getJsonResponse(params);
    }

    private JSONObject getECBlock() {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getECBlock");
        return getJsonResponse(params);
    }

    private JSONObject getJsonResponse(Map<String, String> params) {
        JSONObject response;
        HttpURLConnection connection = null;
        String host = Convert.emptyToNull(Conch.getStringProperty("sharder.mint.serverAddress"));
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                host = "localhost";
            }
        }
        String protocol = "http";
        boolean useHttps = Conch.getBooleanProperty("sharder.mint.useHttps");
        if (useHttps) {
            protocol = "https";
            HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllSSLProvider.getSslSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TrustAllSSLProvider.getHostNameVerifier());
        }
        int port = Conch.getApiPort();
        String urlParams = getUrlParams(params);
        URL url;
        try {
            url = new URL(protocol, host, port, "/sharder?" + urlParams);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        try {
            Logger.logDebugMessage("Sending request to server: " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    response = (JSONObject) JSONValue.parse(reader);
                }
            } else {
                response = null;
            }
        } catch (RuntimeException | IOException e) {
            Logger.logInfoMessage("Error connecting to server", e);
            if (connection != null) {
                connection.disconnect();
            }
            throw new IllegalStateException(e);
        }
        if (response == null) {
            throw new IllegalStateException(String.format("Request %s response error", url));
        }
        if (response.get("errorCode") != null) {
            throw new IllegalStateException(String.format("Request %s produced error response code %s message \"%s\"",
                    url, response.get("errorCode"), response.get("errorDescription")));
        }
        if (response.get("error") != null) {
            throw new IllegalStateException(String.format("Request %s produced error %s",
                    url, response.get("error")));
        }
        return response;
    }

    private static String getUrlParams(Map<String, String> params) {
        if (params == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            try {
                sb.append(key).append("=").append(URLEncoder.encode(params.get(key), "utf8")).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        String rc = sb.toString();
        if (rc.endsWith("&")) {
            rc = rc.substring(0, rc.length() - 1);
        }
        return rc;
    }

    private static class HashSolver implements Callable<Long> {

        private final HashFunction hashFunction;
        private final long currencyId;
        private final long accountId;
        private final long counter;
        private final long units;
        private final long nonce;
        private final byte[] target;
        private final int poolSize;

        private HashSolver(byte algorithm, long currencyId, long accountId, long counter, long units, long nonce,
                           byte[] target, int poolSize) {
            this.hashFunction = HashFunction.getHashFunction(algorithm);
            this.currencyId = currencyId;
            this.accountId = accountId;
            this.counter = counter;
            this.units = units;
            this.nonce = nonce;
            this.target = target;
            this.poolSize = poolSize;
        }

        @Override
        public Long call() {
            long n = nonce;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] hash = CurrencyMinting.getHash(hashFunction, n, currencyId, units, counter, accountId);
                if (CurrencyMinting.meetsTarget(hash, target)) {
                    Logger.logDebugMessage("%s found solution hash %s nonce %d currencyId %d units %d counter %d accountId %d" +
                            " hash %s meets target %s",
                            Thread.currentThread().getName(), hashFunction, n, currencyId, units, counter, accountId,
                            Arrays.toString(hash), Arrays.toString(target));
                    return n;
                }
                n+=poolSize;
                if (((n - nonce) % (poolSize * 1000000)) == 0) {
                    Logger.logInfoMessage("%s computed %d [MH]", Thread.currentThread().getName(), (n - nonce) / poolSize / 1000000);
                }
            }
            return null;
        }
    }
}