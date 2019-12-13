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
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
    var _password = null;
    var accountDetailsModal = $("#account_details_modal");

    accountDetailsModal.on("show.bs.modal", function(e) {
        if (_password) {
            $("#account_details_modal_account_display").show();
            $("#account_details_modal_passphrase_display").show();
        } else {
            NRS.generateQRCode("#account_details_modal_account_qr_code", NRS.accountRS);
            $("#account_details_modal_account_display").hide();
            $("#account_details_modal_passphrase_display").hide();
            $("#account_details_modal_passphrase_qr_code").html($.t("sso.passphrase_not_specified"));
        }
		$("#account_details_modal_balance").show();

        var accountBalanceWarning = $("#account_balance_warning");
        if (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode != 5) {
			$("#account_balance_table").hide();
			accountBalanceWarning.html(NRS.escapeRespStr(NRS.accountInfo.errorDescription)).show();
		} else {
			accountBalanceWarning.hide();
            var accountBalancePublicKey = $("#account_balance_public_key");
            if (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode == 5) {
				$("#account_balance_balance, #account_balance_unconfirmed_balance, #account_balance_effective_balance, #account_balance_guaranteed_balance, #account_balance_forged_balance").html("0 MWFS");
				accountBalancePublicKey.html(NRS.escapeRespStr(NRS.publicKey));
				$("#account_balance_account_rs").html(NRS.getAccountLink(NRS, "account", undefined, undefined, true));
				$("#account_balance_account").html(NRS.escapeRespStr(NRS.account));
			} else {
				$("#account_balance_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.balanceNQT)) + " MWFS");
				$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.unconfirmedBalanceNQT)) + " MWFS");
				$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountInfo.effectiveBalanceNXT) + " MWFS");
				$("#account_balance_guaranteed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.guaranteedBalanceNQT)) + " MWFS");
				$("#account_balance_forged_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.forgedBalanceNQT)) + " MWFS");

				accountBalancePublicKey.html(NRS.escapeRespStr(NRS.accountInfo.publicKey));
				$("#account_balance_account_rs").html(NRS.getAccountLink(NRS.accountInfo, "account", undefined, undefined, true));
				$("#account_balance_account").html(NRS.escapeRespStr(NRS.account));

				if (!NRS.accountInfo.publicKey) {
					accountBalancePublicKey.html("/");
                    var warning = NRS.publicKey != 'undefined' ? $.t("sso.public_key_not_announced_warning", { "public_key": NRS.publicKey }) : $.t("sso.no_public_key_warning");
					accountBalanceWarning.html(warning + " " + $.t("sso.public_key_actions")).show();
				}
			}
		}

		var $invoker = $(e.relatedTarget);
		var tab = $invoker.data("detailstab");
		if (tab) {
			_showTab(tab)
		}
	});

	function _showTab(tab){
		var tabListItem = $("#account_details_modal li[data-tab=" + tab + "]");
		tabListItem.siblings().removeClass("active");
		tabListItem.addClass("active");
		$(".account_details_modal_content").hide();
		var content = $("#account_details_modal_" + tab);
		content.show();
	}

	accountDetailsModal.find("ul.nav li").click(function(e) {
		e.preventDefault();
		var tab = $(this).data("tab");
		_showTab(tab);
	});

	accountDetailsModal.on("hidden.bs.modal", function() {
		$(this).find(".account_details_modal_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$("#account_details_balance_nav").addClass("active");
		$("#account_details_modal_account_qr_code").empty();
		$("#account_details_modal_passphrase_qr_code").empty();
	});

    NRS.setAccountDetailsPassword = function(password) {
        _password = password;
    };

    $("#account_details_modal_account_display").on("click", function() {
        $("#account_details_modal_account_display").hide();
        $("#account_details_modal_passphrase_display").show();
        $("#account_details_modal_passphrase_qr_code").empty();
        NRS.generateQRCode("#account_details_modal_account_qr_code", NRS.accountRS);
        NRS.generateQRCode("#account_details_modal_account_qr_code", NRS.accountRS);
    });

    $("#account_details_modal_passphrase_display").on("click", function() {
        $("#account_details_modal_passphrase_display").hide();
        $("#account_details_modal_account_display").show();
        $("#account_details_modal_account_qr_code").empty();
        NRS.generateQRCode("#account_details_modal_passphrase_qr_code", _password);
    });

    return NRS;
}(global.client, jQuery));
module.exports = NRS;
