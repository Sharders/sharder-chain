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

package org.conch.http;

import org.conch.Account;
import org.conch.Appendix;
import org.conch.Attachment;
import org.conch.Constants;
import org.conch.ConchException;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.conch.util.Search;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.INCORRECT_DGS_LISTING_DESCRIPTION;
import static org.conch.http.JSONResponses.INCORRECT_DGS_LISTING_NAME;
import static org.conch.http.JSONResponses.INCORRECT_DGS_LISTING_TAGS;
import static org.conch.http.JSONResponses.MISSING_NAME;

public final class DGSListing extends CreateTransaction {

    static final DGSListing instance = new DGSListing();

    private DGSListing() {
        super("messageFile", new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "name", "description", "tags", "quantity", "priceNQT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String tags = Convert.nullToEmpty(req.getParameter("tags"));
        long priceNQT = ParameterParser.getPriceNQT(req);
        int quantity = ParameterParser.getGoodsQuantity(req);

        if (name == null) {
            return MISSING_NAME;
        }
        name = name.trim();
        if (name.length() > Constants.MAX_DGS_LISTING_NAME_LENGTH) {
            return INCORRECT_DGS_LISTING_NAME;
        }

        if (description.length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH) {
            return INCORRECT_DGS_LISTING_DESCRIPTION;
        }

        if (tags.length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH) {
            return INCORRECT_DGS_LISTING_TAGS;
        }

        Appendix.PrunablePlainMessage prunablePlainMessage = (Appendix.PrunablePlainMessage)ParameterParser.getPlainMessage(req, true);
        if (prunablePlainMessage != null) {
            if (prunablePlainMessage.isText()) {
                return MESSAGE_NOT_BINARY;
            }
            byte[] image = prunablePlainMessage.getMessage();
            String mediaType = Search.detectMimeType(image);
            if (mediaType == null || !mediaType.startsWith("image/")) {
                return MESSAGE_NOT_IMAGE;
            }
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.DigitalGoodsListing(name, description, tags, quantity, priceNQT);
        return createTransaction(req, account, attachment);

    }

    private static final JSONStreamAware MESSAGE_NOT_BINARY;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 8);
        response.put("errorDescription", "Only binary message attachments accepted as DGS listing images");
        MESSAGE_NOT_BINARY = JSON.prepare(response);
    }

    private static final JSONStreamAware MESSAGE_NOT_IMAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 9);
        response.put("errorDescription", "Message attachment is not an image");
        MESSAGE_NOT_IMAGE = JSON.prepare(response);
    }

}
