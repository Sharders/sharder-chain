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

package org.conch.user;

import org.conch.*;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Users {

  private static final int TESTNET_UI_PORT = 2875;

  private static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
  private static final Collection<User> allUsers =
      Collections.unmodifiableCollection(users.values());

  private static final AtomicInteger peerCounter = new AtomicInteger();
  private static final ConcurrentMap<String, Integer> peerIndexMap = new ConcurrentHashMap<>();
  private static final ConcurrentMap<Integer, String> peerAddressMap = new ConcurrentHashMap<>();

  private static final AtomicInteger blockCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> blockIndexMap = new ConcurrentHashMap<>();

  private static final AtomicInteger transactionCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> transactionIndexMap = new ConcurrentHashMap<>();

  static final Set<String> allowedUserHosts;

  private static final Server userServer;

  static {
    List<String> allowedUserHostsList = Conch.getStringListProperty("sharder.allowedUserHosts");
    if (!allowedUserHostsList.contains("*")) {
      allowedUserHosts = Collections.unmodifiableSet(new HashSet<>(allowedUserHostsList));
    } else {
      allowedUserHosts = null;
    }

    boolean enableUIServer = Conch.getBooleanProperty("sharder.enableUIServer");
    if (enableUIServer) {
      final int port =
          Constants.isTestnetOrDevnet()
              ? TESTNET_UI_PORT
              : Conch.getIntProperty("sharder.uiServerPort");
      final String host = Conch.getStringProperty("sharder.uiServerHost");
      userServer = new Server();
      ServerConnector connector;

      boolean enableSSL = Conch.getBooleanProperty("sharder.uiSSL");
      if (enableSSL) {
        Logger.logMessage("Using SSL (https) for the user interface server");
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(Conch.getStringProperty("sharder.keyStorePath"));
        sslContextFactory.setKeyStorePassword(
            Conch.getStringProperty("sharder.keyStorePassword", null, true));
        sslContextFactory.addExcludeCipherSuites(
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        sslContextFactory.addExcludeProtocols("SSLv3");
        connector =
            new ServerConnector(
                userServer,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https_config));
      } else {
        connector = new ServerConnector(userServer);
      }

      connector.setPort(port);
      connector.setHost(host);
      connector.setIdleTimeout(Conch.getIntProperty("sharder.uiServerIdleTimeout"));
      connector.setReuseAddress(true);
      userServer.addConnector(connector);

      HandlerList userHandlers = new HandlerList();

      ResourceHandler userFileHandler = new ResourceHandler();
      userFileHandler.setDirectoriesListed(false);
      userFileHandler.setWelcomeFiles(new String[] {"index.html"});
      userFileHandler.setResourceBase(Conch.getStringProperty("sharder.uiResourceBase"));

      userHandlers.addHandler(userFileHandler);

      String javadocResourceBase = Conch.getStringProperty("sharder.javadocResourceBase");
      if (javadocResourceBase != null) {
        ContextHandler contextHandler = new ContextHandler("/doc");
        ResourceHandler docFileHandler = new ResourceHandler();
        docFileHandler.setDirectoriesListed(false);
        docFileHandler.setWelcomeFiles(new String[] {"index.html"});
        docFileHandler.setResourceBase(javadocResourceBase);
        contextHandler.setHandler(docFileHandler);
        userHandlers.addHandler(contextHandler);
      }

      ServletHandler userHandler = new ServletHandler();
      ServletHolder userHolder = userHandler.addServletWithMapping(UserServlet.class, "/sharder");
      userHolder.setAsyncSupported(true);

      if (Conch.getBooleanProperty("sharder.uiServerCORS")) {
        FilterHolder filterHolder =
            userHandler.addFilterWithMapping(CrossOriginFilter.class, "/*", FilterMapping.DEFAULT);
        filterHolder.setInitParameter("allowedHeaders", "*");
        filterHolder.setAsyncSupported(true);
      }

      userHandlers.addHandler(userHandler);

      userHandlers.addHandler(new DefaultHandler());

      userServer.setHandler(userHandlers);
      userServer.setStopAtShutdown(true);

      ThreadPool.runBeforeStart(
          () -> {
            try {
              userServer.start();
              Logger.logMessage("Started user interface server at " + host + ":" + port);
            } catch (Exception e) {
              Logger.logErrorMessage("Failed to start user interface server", e);
              throw new RuntimeException(e.toString(), e);
            }
          },
          true);

    } else {
      userServer = null;
      Logger.logMessage("User interface server not enabled");
    }
  }

  static {
    if (userServer != null) {
      Account.addListener(
          account -> {
            JSONObject response = new JSONObject();
            response.put("response", "setBalance");
            response.put("balanceNQT", account.getUnconfirmedBalanceNQT());
            byte[] accountPublicKey = Account.getPublicKey(account.getId());
            Users.users
                .values()
                .forEach(
                    user -> {
                      if (user.getSecretPhrase() != null
                          && Arrays.equals(user.getPublicKey(), accountPublicKey)) {
                        user.send(response);
                      }
                    });
          },
          Account.Event.UNCONFIRMED_BALANCE);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray removedActivePeers = new JSONArray();
            JSONObject removedActivePeer = new JSONObject();
            removedActivePeer.put("index", Users.getIndex(peer));
            removedActivePeers.add(removedActivePeer);
            response.put("removedActivePeers", removedActivePeers);
            JSONArray removedKnownPeers = new JSONArray();
            JSONObject removedKnownPeer = new JSONObject();
            removedKnownPeer.put("index", Users.getIndex(peer));
            removedKnownPeers.add(removedKnownPeer);
            response.put("removedKnownPeers", removedKnownPeers);
            JSONArray addedBlacklistedPeers = new JSONArray();
            JSONObject addedBlacklistedPeer = new JSONObject();
            addedBlacklistedPeer.put("index", Users.getIndex(peer));
            addedBlacklistedPeer.put("address", peer.getHost());
            addedBlacklistedPeer.put(
                "announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
            addedBlacklistedPeer.put("software", peer.getSoftware());
            addedBlacklistedPeers.add(addedBlacklistedPeer);
            response.put("addedBlacklistedPeers", addedBlacklistedPeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.BLACKLIST);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray removedActivePeers = new JSONArray();
            JSONObject removedActivePeer = new JSONObject();
            removedActivePeer.put("index", Users.getIndex(peer));
            removedActivePeers.add(removedActivePeer);
            response.put("removedActivePeers", removedActivePeers);
            JSONArray addedKnownPeers = new JSONArray();
            JSONObject addedKnownPeer = new JSONObject();
            addedKnownPeer.put("index", Users.getIndex(peer));
            addedKnownPeer.put("address", peer.getHost());
            addedKnownPeer.put(
                "announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
            addedKnownPeer.put("software", peer.getSoftware());
            addedKnownPeers.add(addedKnownPeer);
            response.put("addedKnownPeers", addedKnownPeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.DEACTIVATE);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray removedBlacklistedPeers = new JSONArray();
            JSONObject removedBlacklistedPeer = new JSONObject();
            removedBlacklistedPeer.put("index", Users.getIndex(peer));
            removedBlacklistedPeers.add(removedBlacklistedPeer);
            response.put("removedBlacklistedPeers", removedBlacklistedPeers);
            JSONArray addedKnownPeers = new JSONArray();
            JSONObject addedKnownPeer = new JSONObject();
            addedKnownPeer.put("index", Users.getIndex(peer));
            addedKnownPeer.put("address", peer.getHost());
            addedKnownPeer.put(
                "announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
            addedKnownPeer.put("software", peer.getSoftware());
            addedKnownPeers.add(addedKnownPeer);
            response.put("addedKnownPeers", addedKnownPeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.UNBLACKLIST);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray removedKnownPeers = new JSONArray();
            JSONObject removedKnownPeer = new JSONObject();
            removedKnownPeer.put("index", Users.getIndex(peer));
            removedKnownPeers.add(removedKnownPeer);
            response.put("removedKnownPeers", removedKnownPeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.REMOVE);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray changedActivePeers = new JSONArray();
            JSONObject changedActivePeer = new JSONObject();
            changedActivePeer.put("index", Users.getIndex(peer));
            changedActivePeer.put("downloaded", peer.getDownloadedVolume());
            changedActivePeers.add(changedActivePeer);
            response.put("changedActivePeers", changedActivePeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.DOWNLOADED_VOLUME);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray changedActivePeers = new JSONArray();
            JSONObject changedActivePeer = new JSONObject();
            changedActivePeer.put("index", Users.getIndex(peer));
            changedActivePeer.put("uploaded", peer.getUploadedVolume());
            changedActivePeers.add(changedActivePeer);
            response.put("changedActivePeers", changedActivePeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.UPLOADED_VOLUME);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray changedActivePeers = new JSONArray();
            JSONObject changedActivePeer = new JSONObject();
            changedActivePeer.put("index", Users.getIndex(peer));
            changedActivePeer.put("weight", peer.getWeight());
            changedActivePeers.add(changedActivePeer);
            response.put("changedActivePeers", changedActivePeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.WEIGHT);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray removedKnownPeers = new JSONArray();
            JSONObject removedKnownPeer = new JSONObject();
            removedKnownPeer.put("index", Users.getIndex(peer));
            removedKnownPeers.add(removedKnownPeer);
            response.put("removedKnownPeers", removedKnownPeers);
            JSONArray addedActivePeers = new JSONArray();
            JSONObject addedActivePeer = new JSONObject();
            addedActivePeer.put("index", Users.getIndex(peer));
            if (peer.getState() != Peer.State.CONNECTED) {
              addedActivePeer.put("disconnected", true);
            }
            addedActivePeer.put("address", peer.getHost());
            addedActivePeer.put(
                "announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
            addedActivePeer.put("weight", peer.getWeight());
            addedActivePeer.put("downloaded", peer.getDownloadedVolume());
            addedActivePeer.put("uploaded", peer.getUploadedVolume());
            addedActivePeer.put("software", peer.getSoftware());
            addedActivePeers.add(addedActivePeer);
            response.put("addedActivePeers", addedActivePeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.ADDED_ACTIVE_PEER);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray changedActivePeers = new JSONArray();
            JSONObject changedActivePeer = new JSONObject();
            changedActivePeer.put("index", Users.getIndex(peer));
            changedActivePeer.put(
                peer.getState() == Peer.State.CONNECTED ? "connected" : "disconnected", true);
            changedActivePeer.put(
                "announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
            changedActivePeers.add(changedActivePeer);
            response.put("changedActivePeers", changedActivePeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.CHANGED_ACTIVE_PEER);

      Peers.addListener(
          peer -> {
            JSONObject response = new JSONObject();
            JSONArray addedKnownPeers = new JSONArray();
            JSONObject addedKnownPeer = new JSONObject();
            addedKnownPeer.put("index", Users.getIndex(peer));
            addedKnownPeer.put("address", peer.getHost());
            addedKnownPeer.put(
                "announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
            addedKnownPeer.put("software", peer.getSoftware());
            addedKnownPeers.add(addedKnownPeer);
            response.put("addedKnownPeers", addedKnownPeers);
            Users.sendNewDataToAll(response);
          },
          Peers.Event.NEW_PEER);

      Conch.getTransactionProcessor()
          .addListener(
              transactions -> {
                JSONObject response = new JSONObject();
                JSONArray removedUnconfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                  JSONObject removedUnconfirmedTransaction = new JSONObject();
                  removedUnconfirmedTransaction.put("index", Users.getIndex(transaction));
                  removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);
                }
                response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);
                Users.sendNewDataToAll(response);
              },
              TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);

      Conch.getTransactionProcessor()
          .addListener(
              transactions -> {
                JSONObject response = new JSONObject();
                JSONArray addedUnconfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                  JSONObject addedUnconfirmedTransaction = new JSONObject();
                  addedUnconfirmedTransaction.put("index", Users.getIndex(transaction));
                  addedUnconfirmedTransaction.put("timestamp", transaction.getTimestamp());
                  addedUnconfirmedTransaction.put("deadline", transaction.getDeadline());
                  addedUnconfirmedTransaction.put(
                      "recipient", Long.toUnsignedString(transaction.getRecipientId()));
                  addedUnconfirmedTransaction.put("amountNQT", transaction.getAmountNQT());
                  addedUnconfirmedTransaction.put("feeNQT", transaction.getFeeNQT());
                  addedUnconfirmedTransaction.put(
                      "sender", Long.toUnsignedString(transaction.getSenderId()));
                  addedUnconfirmedTransaction.put("id", transaction.getStringId());
                  addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);
                }
                response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);
                Users.sendNewDataToAll(response);
              },
              TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);

      Conch.getTransactionProcessor()
          .addListener(
              transactions -> {
                JSONObject response = new JSONObject();
                JSONArray addedConfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                  JSONObject addedConfirmedTransaction = new JSONObject();
                  addedConfirmedTransaction.put("index", Users.getIndex(transaction));
                  addedConfirmedTransaction.put("blockTimestamp", transaction.getBlockTimestamp());
                  addedConfirmedTransaction.put("transactionTimestamp", transaction.getTimestamp());
                  addedConfirmedTransaction.put(
                      "sender", Long.toUnsignedString(transaction.getSenderId()));
                  addedConfirmedTransaction.put(
                      "recipient", Long.toUnsignedString(transaction.getRecipientId()));
                  addedConfirmedTransaction.put("amountNQT", transaction.getAmountNQT());
                  addedConfirmedTransaction.put("feeNQT", transaction.getFeeNQT());
                  addedConfirmedTransaction.put("id", transaction.getStringId());
                  addedConfirmedTransactions.add(addedConfirmedTransaction);
                }
                response.put("addedConfirmedTransactions", addedConfirmedTransactions);
                Users.sendNewDataToAll(response);
              },
              TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);

      Conch.getBlockchainProcessor()
          .addListener(
              block -> {
                JSONObject response = new JSONObject();
                JSONArray addedOrphanedBlocks = new JSONArray();
                JSONObject addedOrphanedBlock = new JSONObject();
                addedOrphanedBlock.put("index", Users.getIndex(block));
                addedOrphanedBlock.put("timestamp", block.getTimestamp());
                addedOrphanedBlock.put("numberOfTransactions", block.getTransactions().size());
                addedOrphanedBlock.put("totalAmountNQT", block.getTotalAmountNQT());
                addedOrphanedBlock.put("totalFeeNQT", block.getTotalFeeNQT());
                addedOrphanedBlock.put("payloadLength", block.getPayloadLength());
                addedOrphanedBlock.put("generator", Long.toUnsignedString(block.getGeneratorId()));
                addedOrphanedBlock.put("height", block.getHeight());
                addedOrphanedBlock.put("version", block.getVersion());
                addedOrphanedBlock.put("block", block.getStringId());
                addedOrphanedBlock.put(
                    "baseTarget",
                    BigInteger.valueOf(block.getBaseTarget())
                        .multiply(BigInteger.valueOf(100000))
                        .divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET)));
                addedOrphanedBlocks.add(addedOrphanedBlock);
                response.put("addedOrphanedBlocks", addedOrphanedBlocks);
                Users.sendNewDataToAll(response);
              },
              BlockchainProcessor.Event.BLOCK_POPPED);

      Conch.getBlockchainProcessor()
          .addListener(
              block -> {
                JSONObject response = new JSONObject();
                JSONArray addedRecentBlocks = new JSONArray();
                JSONObject addedRecentBlock = new JSONObject();
                addedRecentBlock.put("index", Users.getIndex(block));
                addedRecentBlock.put("timestamp", block.getTimestamp());
                addedRecentBlock.put("numberOfTransactions", block.getTransactions().size());
                addedRecentBlock.put("totalAmountNQT", block.getTotalAmountNQT());
                addedRecentBlock.put("totalFeeNQT", block.getTotalFeeNQT());
                addedRecentBlock.put("payloadLength", block.getPayloadLength());
                addedRecentBlock.put("generator", Long.toUnsignedString(block.getGeneratorId()));
                addedRecentBlock.put("height", block.getHeight());
                addedRecentBlock.put("version", block.getVersion());
                addedRecentBlock.put("block", block.getStringId());
                addedRecentBlock.put(
                    "baseTarget",
                    BigInteger.valueOf(block.getBaseTarget())
                        .multiply(BigInteger.valueOf(100000))
                        .divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET)));
                addedRecentBlocks.add(addedRecentBlock);
                response.put("addedRecentBlocks", addedRecentBlocks);
                Users.sendNewDataToAll(response);
              },
              BlockchainProcessor.Event.BLOCK_PUSHED);

      Generator.addListener(
          generator -> {
            JSONObject response = new JSONObject();
            response.put("response", "setBlockGenerationDeadline");
            response.put("deadline", generator.getDeadline());
            users
                .values()
                .forEach(
                    user -> {
                      if (Arrays.equals(generator.getPublicKey(), user.getPublicKey())) {
                        user.send(response);
                      }
                    });
          },
          Generator.Event.GENERATION_DEADLINE);
    }
  }

  static Collection<User> getAllUsers() {
    return allUsers;
  }

  static User getUser(String userId) {
    User user = users.get(userId);
    if (user == null) {
      user = new User(userId);
      User oldUser = users.putIfAbsent(userId, user);
      if (oldUser != null) {
        user = oldUser;
        user.setInactive(false);
      }
    } else {
      user.setInactive(false);
    }
    return user;
  }

  static User remove(User user) {
    return users.remove(user.getUserId());
  }

  private static void sendNewDataToAll(JSONObject response) {
    response.put("response", "processNewData");
    sendToAll(response);
  }

  private static void sendToAll(JSONStreamAware response) {
    for (User user : users.values()) {
      user.send(response);
    }
  }

  static int getIndex(Peer peer) {
    Integer index = peerIndexMap.get(peer.getHost());
    if (index == null) {
      index = peerCounter.incrementAndGet();
      peerIndexMap.put(peer.getHost(), index);
      peerAddressMap.put(index, peer.getHost());
    }
    return index;
  }

  static Peer getPeer(int index) {
    String peerAddress = peerAddressMap.get(index);
    if (peerAddress == null) {
      return null;
    }
    return Peers.getPeer(peerAddress);
  }

  static int getIndex(Block block) {
    Integer index = blockIndexMap.get(block.getId());
    if (index == null) {
      index = blockCounter.incrementAndGet();
      blockIndexMap.put(block.getId(), index);
    }
    return index;
  }

  static int getIndex(Transaction transaction) {
    Integer index = transactionIndexMap.get(transaction.getId());
    if (index == null) {
      index = transactionCounter.incrementAndGet();
      transactionIndexMap.put(transaction.getId(), index);
    }
    return index;
  }

  public static void init() {}

  public static void shutdown() {
    if (userServer != null) {
      try {
        userServer.stop();
      } catch (Exception e) {
        Logger.logShutdownMessage("Failed to stop user interface server", e);
      }
    }
  }

  private Users() {} // never
}
