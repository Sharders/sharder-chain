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

    NRS.forms.dividendPayment = function($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        data.asset = NRS.getCurrentAsset().asset;
        if (!data.amountSSPerShare) {
            return {
                "error": $.t("error_amount_per_share_required")
            }
        } else {
            data.amountNQTPerQNT = NRS.calculatePricePerWholeQNT(
                NRS.convertToNQT(data.amountSSPerShare),
                NRS.getCurrentAsset().decimals);
        }
        if (!/^\d+$/.test(data.height)) {
            return {
                "error": $.t("error_invalid_dividend_height")
            }
        }
        var isDividendHeightBeforeAssetHeight;
        NRS.sendRequest("getTransaction", { transaction: data.asset }, function(response) {
            if (response.height > data.height) {
                isDividendHeightBeforeAssetHeight = true;
            }
        }, { isAsync: false });
        if (isDividendHeightBeforeAssetHeight) {
            return {
                "error": $.t("dividend_height_asset_height")
            };
        }
        delete data.amountSSPerShare;
        return {
            "data": data
        };
    };

    $("#dividend_payment_modal").on("hidden.bs.modal", function() {
        $(this).find(".dividend_payment_info").first().hide();
    });

    $("#dividend_payment_amount_per_share").keydown(function(e) {
        var decimals = NRS.getCurrentAsset().decimals;
        var charCode = !e.charCode ? e.which : e.charCode;
        if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
            return;
        }
        return NRS.validateDecimals(8-decimals, charCode, $(this).val(), e);
   	});

    $("#dividend_payment_amount_per_share, #dividend_payment_height").on("blur", function() {
        var $modal = $(this).closest(".modal");
        var amountSSPerShare = $modal.find("#dividend_payment_amount_per_share").val();
        var height = $modal.find("#dividend_payment_height").val();
        var $callout = $modal.find(".dividend_payment_info").first();
        var classes = "callout-info callout-danger callout-warning";
        if (amountSSPerShare && /^\d+$/.test(height)) {
            NRS.getAssetAccounts(NRS.getCurrentAsset().asset, height,
                function (response) {
                    var accountAssets = response.accountAssets;
                    var qualifiedDividendRecipients = accountAssets.filter(
                        function(accountAsset) {
                            return accountAsset.accountRS !== NRS.getCurrentAsset().accountRS
                                && accountAsset.accountRS !== NRS.constants.GENESIS_RS;
                        });
                    var totalQuantityQNT = new BigInteger("0");
                    qualifiedDividendRecipients.forEach(
                        function (accountAsset) {
                            totalQuantityQNT = totalQuantityQNT.add(new BigInteger(accountAsset.quantityQNT));
                        }
                    );
                    var priceNQT = new BigInteger(NRS.calculatePricePerWholeQNT(NRS.convertToNQT(amountSSPerShare), NRS.getCurrentAsset().decimals));
                    var totalSS = NRS.calculateOrderTotal(totalQuantityQNT, priceNQT);
                    $callout.html($.t("dividend_payment_info_preview_success",
                            {
                                "amountSS": totalSS,
                                "totalQuantity": NRS.formatQuantity(totalQuantityQNT, NRS.getCurrentAsset().decimals),
                                "recipientCount": qualifiedDividendRecipients.length
                            })
                    );
                    $callout.removeClass(classes).addClass("callout-info").show();
                },
                function (response) {
                    var displayString;
                    if (response.errorCode == 4 || response.errorCode == 8) {
                        displayString = $.t("error_invalid_dividend_height");
                    } else {
                        displayString = $.t("dividend_payment_info_preview_error", {"errorCode": response.errorCode});
                    }
                    $callout.html(displayString);
                    $callout.removeClass(classes).addClass("callout-warning").show();
                }
            );
        }
    });

    return NRS;
}(NRS || {}, jQuery));
