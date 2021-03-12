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

package org.conch.http.biz.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver;
import org.conch.common.ConchException;

import java.lang.reflect.Method;
import java.util.List;

public class BizErrorResolver implements ErrorResolver {
    @Override
    public JsonError resolveError(Throwable throwable, Method method, List<JsonNode> list) {
        JsonError jsonError = null;
        if (throwable instanceof BizParameterException) {
            jsonError = new JsonError(-32000, throwable.getMessage(), list.get(0));
        }else if (throwable instanceof ConchException.InsufficientBalanceException) {
            jsonError = new JsonError(-32001, throwable.getMessage(), list.get(0));
        }

        return jsonError;
    }
}
