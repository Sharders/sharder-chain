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

import java.util.HashMap;
import java.util.Map;

public enum APITag {

    ACCOUNTS("Accounts"), ACCOUNT_CONTROL("Account Control"), ALIASES("Aliases"), AE("Asset Exchange"), BLOCKS("Blocks"),
    CREATE_TRANSACTION("Create Transaction"), DGS("Digital Goods Store"), FORGING("Forging"), MESSAGES("Messages"),
    MS("Monetary System"), NETWORK("Networking"), PHASING("Phasing"), SEARCH("Search"), INFO("Server Info"),
    SHUFFLING("Shuffling"), DATA("Tagged Data"), TOKENS("Tokens"), TRANSACTIONS("Transactions"), VS("Voting System"),
    UTILS("Utils"), DEBUG("Debug"), ADDONS("Add-ons");

    private static final Map<String, APITag> apiTags = new HashMap<>();
    static {
        for (APITag apiTag : values()) {
            if (apiTags.put(apiTag.getDisplayName(), apiTag) != null) {
                throw new RuntimeException("Duplicate APITag name: " + apiTag.getDisplayName());
            }
        }
    }

    public static APITag fromDisplayName(String displayName) {
        APITag apiTag = apiTags.get(displayName);
        if (apiTag == null) {
            throw new IllegalArgumentException("Invalid APITag name: " + displayName);
        }
        return apiTag;
    }

    private final String displayName;

    APITag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
