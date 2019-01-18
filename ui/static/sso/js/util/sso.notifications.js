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

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {

	NRS.updateNotificationUI = function() {
		var subTypeCount = 0;
		var totalCount = 0;

		var $menuItem = $('#notification_menu');
		var $popoverItem = $("<div id='notification_popover'></div>");

		$.each(NRS.transactionTypes, function(typeIndex, typeDict) {
			$.each(typeDict["subTypes"], function(subTypeIndex, subTypeDict) {
				if (subTypeDict["notificationCount"] > 0) {
					subTypeCount += 1;
					totalCount += subTypeDict["notificationCount"];
					var html = "";
					html += "<a href='#' style='display:block;background-color:#f0f0f0;border:1px solid #e2e2e2;padding:4px 12px 9px 12px;margin:2px;'>";
					html += "<div style='float:right;'><div style='display:inline-block;margin-top:2px;'>";
					html += "<span class='badge' style='background-color:#e65;'>" + subTypeDict["notificationCount"] + "</span>";
					html += "</div></div>";
					html += NRS.getTransactionIconHTML(typeIndex, subTypeIndex) + "&nbsp; ";
					html += '<span style="font-size:12px;color:#000;display:inline-block;margin-top:5px;">';
					html += $.t(subTypeDict['i18nKeyTitle'], subTypeDict['title']);
					html += '</span>';
					html += "</a>";

					var $subTypeItem = $(html);
					$subTypeItem.click(function(e) {
						e.preventDefault();
						NRS.goToPage(subTypeDict["receiverPage"]);
						$menuItem.popover('hide');
					});
					$subTypeItem.appendTo($popoverItem);
				}
			});
		});
		if (totalCount > 0) {
			$menuItem.find('span .nm_inner_subtype').css('backgroundColor', '#337ab7');
			$menuItem.find('span .nm_inner_total').css('backgroundColor', '#e06054');

			var $markReadDiv = $("<div style='text-align:center;padding:12px 12px 8px 12px;'></div>");
			var $markReadLink= $("<a href='#' style='color:#3c8dbc;'>" + $.t('sso.notifications_mark_as_read', 'Mark all as read') + "</a>");
			$markReadLink.click(function(e) {
				e.preventDefault();
				NRS.resetNotificationState();
				$menuItem.popover('hide');
			});
			$markReadLink.appendTo($markReadDiv);
			$popoverItem.append($markReadDiv);
			document.title = $.t('sso.app_title') + ' (' + String(totalCount) + ')';
		} else {
			$menuItem.find('span .nm_inner_subtype').css('backgroundColor', '#337ab7');
			$menuItem.find('span .nm_inner_total').css('backgroundColor', '');
			var html = "";
			html += "<div style='text-align:center;padding:12px;'>" + $.t('sso.no_notifications', 'No current notifications') + "</div>";
			$popoverItem.append(html);
			document.title = $.t('sso.app_title');
		}

		$menuItem.find('span .nm_inner_subtype').html(String(subTypeCount));
		$menuItem.find('span .nm_inner_total').html(String(totalCount));
		$menuItem.show();

		var template = '<div class="popover" style="min-width:320px;"><div class="arrow"></div><div class="popover-inner">';
		template += '<h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>';

		if($menuItem.data('bs.popover')) {
    		$menuItem.data('bs.popover').options.content = $popoverItem;
		} else {
			$menuItem.popover({
				"html": true,
				"content": $popoverItem,
				"trigger": "click",
				template: template
			});
		}
	};

	NRS.saveNotificationTimestamps = function() {
		var tsDict = {};
		$.each(NRS.transactionTypes, function(typeIndex, typeDict) {
			$.each(typeDict["subTypes"], function(subTypeIndex, subTypeDict) {
				var tsKey = "ts_" + String(typeIndex) + "_" + String(subTypeIndex);
				tsDict[tsKey] = subTypeDict["notificationTS"];
			});
		});
		var tsDictString = JSON.stringify(tsDict);
		NRS.storageSelect("data", [{
			"id": "notification_timestamps"
		}], function(error, result) {
			if (result && result.length > 0) {
				NRS.storageUpdate("data", {
					contents: tsDictString
				}, [{
					id: "notification_timestamps"
				}]);
			} else {
				NRS.storageInsert("data", "id", {
					id: "notification_timestamps",
					contents: tsDictString
				});
			}
		});
	};

	NRS.resetNotificationState = function(page) {
		NRS.sendRequest("getTime", {}, function(response) {
			if (response.time) {
				$.each(NRS.transactionTypes, function(typeIndex, typeDict) {
					$.each(typeDict["subTypes"], function(subTypeIndex, subTypeDict) {
						if (!page || (page && subTypeDict["receiverPage"] == page)) {
							var countBefore = subTypeDict["notificationCount"];
							if (subTypeDict["lastKnownTransaction"]) {
								subTypeDict["notificationTS"] = subTypeDict["lastKnownTransaction"].timestamp + 1;
							} else {
								subTypeDict["notificationTS"] = response.time;
							}
							subTypeDict["notificationCount"] = 0;
							typeDict["notificationCount"] -= countBefore;
						}
					});
				});
				NRS.saveNotificationTimestamps();
				// NRS.updateNotificationUI();
			}
		});
	};

	// NRS.initNotificationCounts = function(time) {
	// 	var fromTS = time - 60 * 60 * 24 * 14;
	// 	NRS.sendRequest("getBlockchainTransactions+", {
	// 		"account": NRS.account,
	// 		"timestamp": fromTS,
	// 		"firstIndex": 0,
	// 		"lastIndex": 99
	// 	}, function(response) {
	// 		if (response.transactions && response.transactions.length) {
	// 			for (var i=0; i<response.transactions.length; i++) {
	// 				var t = response.transactions[i];
	// 				var subTypeDict = NRS.transactionTypes[t.type]["subTypes"][t.subtype];
	// 				if (t.recipient && t.recipient == NRS.account && subTypeDict["receiverPage"]) {
	// 					if (!subTypeDict["lastKnownTransaction"] || subTypeDict["lastKnownTransaction"].timestamp < t.timestamp) {
	// 						subTypeDict["lastKnownTransaction"] = t;
	// 					}
	// 					if (t.timestamp > subTypeDict["notificationTS"]) {
	// 						NRS.transactionTypes[t.type]["notificationCount"] += 1;
	// 						subTypeDict["notificationCount"] += 1;
	// 					}
	// 				}
	// 			}
	// 		}
	// 		// NRS.updateNotificationUI();
	// 	});
	// };

	NRS.loadNotificationsFromTimestamps = function(time, tsDictString) {
		var tsDict = {};
		if (tsDictString != "") {
			tsDict = JSON.parse(tsDictString);
		}

		$.each(NRS.transactionTypes, function(typeIndex, typeDict) {
			typeDict["notificationCount"] = 0;
			$.each(typeDict["subTypes"], function(subTypeIndex, subTypeDict) {
				var tsKey = "ts_" + String(typeIndex) + "_" + String(subTypeIndex);
				if (tsDict[tsKey]) {
					subTypeDict["notificationTS"] = tsDict[tsKey];
				} else {
					subTypeDict["notificationTS"] = time;
				}
				subTypeDict["notificationCount"] = 0;
			});
		});
		// NRS.initNotificationCounts(time);
		NRS.saveNotificationTimestamps();
	};

	NRS.updateNotifications = function() {
		NRS.sendRequest("getTime", {}, function(response) {
			if (response.time) {
				var tsDictString = "";
				NRS.storageSelect("data", [{
					"id": "notification_timestamps"
				}], function(error, result) {
					//console.log(result);
					if (result) {
						if (result.length > 0) {
							tsDictString = result[0].contents;
							NRS.loadNotificationsFromTimestamps(response.time, tsDictString);
						} else {
							NRS.loadNotificationsFromTimestamps(response.time, "");
						}
					}
				});
			}
		});
	};

	NRS.setUnconfirmedNotifications = function() {
		$('#unconfirmed_notification_counter').html(String(NRS.unconfirmedTransactions.length));
		$('#unconfirmed_notification_menu').show();
	};

	NRS.setPhasingNotifications = function() {
		NRS.sendRequest("getAccountPhasedTransactionCount", {
			"account": NRS.account
		}, function(response) {
			//noinspection JSUnresolvedVariable
			if (response.numberOfPhasedTransactions != undefined) {
				//noinspection JSUnresolvedVariable
				$('#phasing_notification_counter').html(String(response.numberOfPhasedTransactions));
				$('#phasing_notification_menu').show();
			}
		});
	};


	return NRS;
}(global.client, jQuery));
module.exports = NRS;
