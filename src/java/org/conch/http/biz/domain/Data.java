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

package org.conch.http.biz.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.conch.Account;
import org.conch.Constants;
import org.conch.util.Convert;

/**
 * Data
 *
 * @author bubai
 * @date 2018/3/23
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY
)
public class Data {
    private String name;
    private String channel;
    private String channelName;
    private String channelDesc;
    private String data;
    private String type;
    @JsonIgnore
    private String hash;

    @JsonProperty(value = "fileName")
    public String getName() {
        return name;
    }

    @JsonProperty(value = "name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty(value = "clientAccount")
    public String getChannel() {
        return channel;
    }

    @JsonProperty(value = "channel")
    public void setChannel(String channel) {
        this.channel = channel;
        setAccountInfo();
    }

    @JsonProperty(value = "clientAccountName")
    public String getChannelName() {
        return channelName;
    }

    @JsonProperty(value = "clientAccountDesc")
    public String getChannelDesc() {
        return channelDesc;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @JsonProperty(value = "fileType")
    public String getType() {
        return type;
    }

    @JsonProperty(value = "type")
    public void setType(String type) {
        this.type = type;
    }

    private void setAccountInfo() {
        if(!this.channel.startsWith(Constants.ACCOUNT_PREFIX)) return;
        Account account = Account.getAccount(Convert.parseAccountId(this.channel));
        if(account != null) {
            Account.AccountInfo info = account.getAccountInfo();
            if (info != null) {
                this.channelName = info.getName();
                this.channelDesc = info.getDescription();
            }
        }
    }
}
