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
import org.conch.Attachment;
import org.conch.ConchException;
import org.conch.Constants;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.NOT_ENABLE_ON_LIGHTCLIENT;

public final class StoreData extends CreateTransaction {

    static final StoreData instance = new StoreData();

    private StoreData() {
        super("file", new APITag[] {APITag.DATA_STORAGE, APITag.CREATE_TRANSACTION},
                "name", "description", "type", "channel", "data", "existence_height", "replicated_number");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        if (Constants.isLightClient) {
            throw new ParameterException(NOT_ENABLE_ON_LIGHTCLIENT);
        }
        Account account = ParameterParser.getSenderAccount(req);
        Attachment.DataStorageUpload dataStorage = ParameterParser.getDataStorage(req);
        return createTransaction(req, account, dataStorage);

    }

}
