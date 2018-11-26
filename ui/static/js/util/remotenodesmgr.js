/******************************************************************************
 * Copyright © 2017 sharder.org.                             *
 * Copyright © 2014-2017 ichaoj.com.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,*
 * no part of the COS software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

function RemoteNode(peerData) {
    this.address = peerData.address;
    this.announcedAddress = peerData.announcedAddress;
    this.port = peerData.apiPort;
    this.isSsl = peerData.isSsl ? true : false; // For now only nodes specified by the user can use SSL since we need trusted certificate
    this.blacklistedUntil = 0;
    this.connectionTime = new Date();
}

RemoteNode.prototype.getUrl = function () {
    return (this.isSsl ? "https://" : "http://") + this.address + ":" + this.port;
};

RemoteNode.prototype.isBlacklisted = function () {
    return new Date().getTime() < this.blacklistedUntil;
};

RemoteNode.prototype.blacklist = function () {
    var blacklistedUntil = new Date().getTime() + 10 * 60 * 1000;
    NRS.logConsole("Blacklist " + this.address + " until " + new Date(blacklistedUntil).format("isoDateTime"));
    this.blacklistedUntil = blacklistedUntil;
};

function RemoteNodesManager(isTestnet) {
    this.isTestnet = isTestnet;
    this.nodes = {};
    // Bootstrap connections
    this.bc = {
        success: 0, // Successful connections counter
        fail: 0, // Failed connections counter
        counter: 0, // Connection attempts counter
        target: 0, // Target number of successful connections
        limit: 0 // Total number of connection attempts
    };
    this.init();
}

function isValidVersion(version) {
    var parts = String(version).split(".");
    //current valid version is 0.0.7
    if (parts.length == 3) {
        if (parseInt(parts[0], 10) > 0) {
            return false;
        }
        if (parseInt(parts[1], 10) > 0) {
            return false;
        }
        return parseInt(parts[2], 10) >= 7;
    } else {
        return false;
    }
}

function isRemoteNodeConnectable(nodeData, isSslAllowed) {
    if (nodeData.services instanceof Array && (nodeData.services.indexOf("API") >= 0 || (isSslAllowed && nodeData.services.indexOf("API_SSL") >= 0))) {
        if (!NRS.isRequireCors() || nodeData.services.indexOf("CORS") >= 0) {
            return isValidVersion(nodeData.version);
        }
    }
    return false;
}

RemoteNodesManager.prototype.addRemoteNodes = function (peersData) {
    var mgr = this;
    $.each(peersData, function(index, peerData) {
        if (isRemoteNodeConnectable(peerData, false)) {
            var oldNode = mgr.nodes[peerData.address];
            var newNode = new RemoteNode(peerData);
            if (oldNode) {
                newNode.blacklistedUntil = oldNode.blacklistedUntil;
            }
            mgr.nodes[peerData.address] = newNode;
            NRS.logConsole("Found remote node " + peerData.address + " blacklisted " + newNode.isBlacklisted());
        }
    });
};

RemoteNodesManager.prototype.addBootstrapNode = function (resolve, reject) {
    var node = new RemoteNode({
        address: NRS.mobileSettings.remote_node_address,
        announcedAddress: NRS.mobileSettings.remote_node_address,
        apiPort: NRS.mobileSettings.remote_node_port,
        isSsl: NRS.mobileSettings.is_remote_node_ssl
    });
    var mgr = this;
    NRS.logConsole("Connecting to configured address " + node.address + " on port " + node.port + " using ssl " + node.isSsl);
    NRS.sendRequest("getBlockchainStatus", { "_extra": node }, function(response, data) {
        if (response.blockchainState && response.blockchainState != "UP_TO_DATE" || response.isDownloading) {
            NRS.logConsole("Warning: Bootstrap node blockchain state is " + response.blockchainState);
        }
        if (response.errorCode || !isRemoteNodeConnectable(response, true)) {
            if (response.errorCode) {
                NRS.logConsole("Bootstrap node cannot be used " + response.errorDescription);
            } else {
                NRS.logConsole("Bootstrap node does not provide the required services");
            }
            $.growl("Cannot connect to configured node, connecting to a random node");
            mgr.addBootstrapNodes(resolve, reject);
            return;
        }
        var node = data["_extra"];
        NRS.logConsole("Adding bootstrap node " + node.address);
        mgr.nodes[node.address] = node;
        resolve();
    }, { noProxy: true, remoteNode: node });
};

RemoteNodesManager.prototype.addBootstrapNodes = function (resolve, reject) {
    NRS.logConsole("addBootstrapNodes: client protocol is '" + window.location.protocol + "'");
    if (!NRS.isRemoteNodeConnectionAllowed()) {
        NRS.logConsole($.t("https_client_cannot_connect_remote_nodes"));
        $.growl($.t("https_client_cannot_connect_remote_nodes"));
        var mobileSettingsModal = $("#mobile_settings_modal");
        mobileSettingsModal.find("input[name=is_offline]").val("true");
        mobileSettingsModal.modal("show");
        return false;
    }
    var peersData = this.REMOTE_NODES_BOOTSTRAP.peers;
    peersData = NRS.getRandomPermutation(peersData);
    var mgr = this;
    mgr.bc.target = NRS.mobileSettings.is_testnet ? 2 : NRS.mobileSettings.bootstrap_nodes_count;
    mgr.bc.limit = Math.min(peersData.length, 3*mgr.bc.target);
    var data = {state: "CONNECTED", includePeerInfo: true};

    function countRejections() {
        mgr.bc.fail ++;
        return mgr.bc.fail >= mgr.bc.limit;
    }

    for (var i=0; i < mgr.bc.limit; i++) {
        var peerData = peersData[i];
        if (!isRemoteNodeConnectable(peerData, false)) {
            NRS.logConsole("Reject: bootstrap node " + peerData.address + " required services not available" +
                (peerData.services ? ", node services " + peerData.services : ""));
            if (countRejections()) {
                reject();
            }
            mgr.bc.counter ++;
            continue;
        }
        var node = new RemoteNode(peerData);
        if (!node.port) {
            NRS.logConsole("Reject: bootstrap node " + node.address + ", api port undefined");
            if (countRejections()) {
                reject();
            }
            mgr.bc.counter ++;
            continue;
        }
        data["_extra"] = node;
        NRS.logConsole("Connecting to bootstrap node " + node.address + " port " + node.port);
        NRS.sendRequest("getBlockchainStatus", data, function(response, data) {
            mgr.bc.counter ++;
            if (mgr.bc.success >= mgr.bc.target) {
                NRS.logConsole("Ignore: already have enough nodes, bootstrap node not added");
                resolve();
                return;
            }
            if (response.errorCode) {
                // Here we don't know which node it was
                NRS.logConsole("Reject: bootstrap node returned error " + response.errorDescription);
                if (countRejections()) {
                    reject();
                }
                return;
            }
            var responseNode = data["_extra"];
            if (response.blockchainState && response.blockchainState != "UP_TO_DATE" || response.isDownloading) {
                NRS.logConsole("Reject: bootstrap node " + responseNode.address + " blockchain state is " + response.blockchainState);
                if (countRejections()) {
                    reject();
                }
                return;
            }
            if (!isRemoteNodeConnectable(response, false)) {
                NRS.logConsole("Reject: bootstrap node " + responseNode.address + " required service not available, node services " + responseNode.services);
                if (countRejections()) {
                    reject();
                }
                return;
            }
            NRS.logConsole("Accept: adding bootstrap node " + responseNode.address + " response time " + (new Date() - responseNode.connectionTime) + " ms");
            mgr.nodes[responseNode.address] = responseNode;
            mgr.bc.success ++;
            if (mgr.bc.success == mgr.bc.target) {
                NRS.logConsole("Resolve: found " + mgr.bc.target + " nodes, start client");
                resolve();
            } else if (mgr.bc.counter == mgr.bc.limit) {
                NRS.logConsole("Connection failed, connected only to " + mgr.bc.success + " nodes in " + mgr.bc.counter + " attempts. Target is " + mgr.bc.target);
                reject();
            }
        }, { noProxy: true, remoteNode: node });
    }
};

RemoteNodesManager.prototype.getRandomNode = function (ignoredAddresses) {
    var addresses = Object.keys(this.nodes);
    if (addresses.length == 0) {
        NRS.logConsole("Cannot get random node. No nodes available");
        return null;
    }
    var index = Math.floor((Math.random() * addresses.length));
    var startIndex = index;
    var node;
    do {
        var address = addresses[index];
        if (ignoredAddresses instanceof Array && ignoredAddresses.indexOf(address) >= 0) {
            node = null;
        } else {
            node = this.nodes[address];
            if (node != null && node.isBlacklisted()) {
                node = null;
            }
        }
        index = (index+1) % addresses.length;
    } while(node == null && index != startIndex);

    return node;
};

RemoteNodesManager.prototype.getRandomNodes = function (count, ignoredAddresses) {
    var processedAddresses = [];
    if (ignoredAddresses instanceof Array) {
        processedAddresses.concat(ignoredAddresses)
    }

    var result = [];
    for (var i = 0; i < count; i++) {
        var node = this.getRandomNode(processedAddresses);
        if (node) {
            processedAddresses.push(node.address);
            result.push(node);
        }
    }
    return result;
};

RemoteNodesManager.prototype.findMoreNodes = function (isReschedule) {
    var nodesMgr = this;
    var node = this.getRandomNode();
    if (node == null) {
        return;
    }
    var data = {state: "CONNECTED", includePeerInfo: true};
    NRS.sendRequest("getPeers", data, function (response) {
        if (response.peers) {
            nodesMgr.addRemoteNodes(response.peers);
        }
        if (isReschedule) {
            setTimeout(function () {
                nodesMgr.findMoreNodes(true);
            }, 30000);
        }
    }, { noProxy: true, remoteNode: node });
};

RemoteNodesManager.prototype.init = function () {
    if (NRS.isMobileApp()) {
        //load the remote nodes bootstrap file only for mobile wallet
        jQuery.ajaxSetup({ async: false });
        $.getScript(this.isTestnet ? "js/data/remotenodesbootstrap.testnet.js" : "js/data/remotenodesbootstrap.mainnet.js");
        jQuery.ajaxSetup({async: true});
    }
};
