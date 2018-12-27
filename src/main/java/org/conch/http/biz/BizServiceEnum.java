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

package org.conch.http.biz;

import org.conch.http.*;
import org.conch.http.biz.service.*;

import java.util.*;

public enum BizServiceEnum {
    //To preserve compatibility, please add new APIs to the end of the enum.
    //When an API is deleted, set its name to empty string and handler to null.
    TEST_SERVICE(new TestServiceImpl(), TestService.class),
    ACCOUNT_SERVICE(new AccountServiceImpl(), AccountService.class);
    static {
        List<String> normalAPINames = new ArrayList<>();
        for (APIEnum api : APIEnum.values()) {
            normalAPINames.add(api.getName());
        }
    }

    private final Service serviceImpl;
    private final Class serviceInterface;

    BizServiceEnum(Service serviceImpl, Class serviceInterface) {
        this.serviceImpl = serviceImpl;
        this.serviceInterface = serviceInterface;
    }

    public Service getServiceImpl() {
        return serviceImpl;
    }

    public Class getServiceInterface() {
        return serviceInterface;
    }
}
