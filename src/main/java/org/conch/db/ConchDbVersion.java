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

package org.conch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.conch.common.Constants;
import org.conch.util.Convert;

public class ConchDbVersion extends DbVersion {

    protected void update(int nextUpdate) {
        Connection con = null;
        try {
            con = db.getConnection();
            switch (nextUpdate) {
                case 1:
                    apply("CREATE TABLE IF NOT EXISTS block (db_id IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
                            + "timestamp INT NOT NULL, previous_block_id BIGINT, "
                            + "total_amount BIGINT NOT NULL, "
                            + "total_fee BIGINT NOT NULL, payload_length INT NOT NULL, "
                            + "previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, "
                            + "next_block_id BIGINT, "
                            + "height INT NOT NULL, generation_signature BINARY(64) NOT NULL, "
                            + "block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_id BIGINT NOT NULL, ext BINARY(237));"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height);"
                            + "CREATE INDEX IF NOT EXISTS block_generator_id_idx ON block (generator_id);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC);"
                            + "ALTER TABLE block DROP COLUMN IF EXISTS generator_public_key;"
                            + "ALTER TABLE block ADD COLUMN IF NOT EXISTS ext BINARY(237);"
                            + "ALTER TABLE BLOCK ADD COLUMN IF NOT EXISTS HAS_REWARD_DISTRIBUTION BOOLEAN NOT NULL DEFAULT false;"
                    );
                case 2:
                    apply("CREATE TABLE IF NOT EXISTS transaction (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "deadline SMALLINT NOT NULL, recipient_id BIGINT, "
                            + "amount BIGINT NOT NULL, fee BIGINT NOT NULL, full_hash BINARY(32) NOT NULL, "
                            + "height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                            + "signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, "
                            + "sender_id BIGINT NOT NULL, block_timestamp INT NOT NULL, referenced_transaction_full_hash BINARY(32), "
                            + "transaction_index SMALLINT NOT NULL, phased BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "attachment_bytes VARBINARY, version TINYINT NOT NULL, has_message BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE, has_prunable_attachment BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "ec_block_height INT DEFAULT NULL, ec_block_id BIGINT DEFAULT NULL, has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id);"
                            + "CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON transaction (sender_id);"
                            + "CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id);"
                            + "CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC);"
                            + "ALTER TABLE transaction ADD COLUMN IF NOT EXISTS transaction_index SMALLINT NOT NULL;"
                            + "ALTER TABLE transaction DROP COLUMN IF EXISTS sender_public_key;"
                            + "ALTER TABLE transaction ADD COLUMN IF NOT EXISTS phased BOOLEAN NOT NULL DEFAULT FALSE;"
                            + "ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE;"
                            + "ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_prunable_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE;"
                            + "ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_prunable_attachment BOOLEAN NOT NULL DEFAULT FALSE;"
                    );
                case 3:
                    apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY, last_updated INT, services BIGINT);"
                            + "ALTER TABLE peer ADD COLUMN IF NOT EXISTS last_updated INT;"
                            + "ALTER TABLE peer ADD COLUMN IF NOT EXISTS services BIGINT;"
                    );
                case 4:
                    apply("CREATE TABLE IF NOT EXISTS alias (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
                            + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
                            + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON alias (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS alias_account_id_idx ON alias (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON alias (alias_name_lower);"
                            + "CREATE INDEX IF NOT EXISTS alias_height_id_idx ON alias (height, id);"
                    );
                case 5:
                    apply("CREATE TABLE IF NOT EXISTS alias_offer (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "price BIGINT NOT NULL, buyer_id BIGINT, "
                            + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS alias_offer_height_id_idx ON alias_offer (height, id);"
                    );
                case 6:
                    apply("CREATE TABLE IF NOT EXISTS asset (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL, "
                            + "initial_quantity BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id);"
                            + "ALTER TABLE asset ADD COLUMN IF NOT EXISTS latest BOOLEAN NOT NULL DEFAULT TRUE;"
                            + "ALTER TABLE asset ADD COLUMN IF NOT EXISTS initial_quantity BIGINT NOT NULL;"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS asset_id_height_idx ON asset (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_height_id_idx ON asset (height, id);"
                    );
                case 7:
                    apply("CREATE TABLE IF NOT EXISTS trade (db_id IDENTITY, asset_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                            + "ask_order_id BIGINT NOT NULL, bid_order_id BIGINT NOT NULL, ask_order_height INT NOT NULL, "
                            + "bid_order_height INT NOT NULL, seller_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                            + "is_buy BOOLEAN NOT NULL, "
                            + "quantity BIGINT NOT NULL, price BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL);"
                            + "CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS trade_seller_id_idx ON trade (seller_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS trade_buyer_id_idx ON trade (buyer_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS trade_height_idx ON trade(height);"
                            + "ALTER TABLE trade ADD COLUMN IF NOT EXISTS is_buy BOOLEAN NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS trade_ask_idx ON trade (ask_order_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS trade_bid_idx ON trade (bid_order_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS trade_height_db_id_idx ON trade (height DESC, db_id DESC);"
                    );
                case 8:
                    apply("CREATE TABLE IF NOT EXISTS ask_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, transaction_index SMALLINT NOT NULL, transaction_height INT NOT NULL, "
                            + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
                            + "latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ask_order (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ask_order (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ask_order (asset_id, price);"
                            + "CREATE INDEX IF NOT EXISTS ask_order_creation_idx ON ask_order (creation_height DESC);"
                            + "ALTER TABLE ask_order ADD COLUMN IF NOT EXISTS transaction_index SMALLINT NOT NULL;"
                            + "ALTER TABLE ask_order ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS ask_order_height_id_idx ON ask_order (height, id);"
                    );
                case 9:
                    apply("CREATE TABLE IF NOT EXISTS bid_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, transaction_index SMALLINT NOT NULL, transaction_height INT NOT NULL, "
                            + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
                            + "latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON bid_order (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON bid_order (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON bid_order (asset_id, price DESC);"
                            + "CREATE INDEX IF NOT EXISTS bid_order_creation_idx ON bid_order (creation_height DESC);"
                            + "ALTER TABLE bid_order ADD COLUMN IF NOT EXISTS transaction_index SMALLINT NOT NULL;"
                            + "ALTER TABLE bid_order ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS bid_order_height_id_idx ON bid_order (height, id);"
                    );
                case 10:
                    apply("CREATE TABLE IF NOT EXISTS goods (db_id IDENTITY, id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                            + "name VARCHAR NOT NULL, description VARCHAR, parsed_tags ARRAY, "
                            + "tags VARCHAR, timestamp INT NOT NULL, quantity INT NOT NULL, price BIGINT NOT NULL, "
                            + "delisted BOOLEAN NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS goods_id_height_idx ON goods (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS goods_seller_id_name_idx ON goods (seller_id, name);"
                            + "CREATE INDEX IF NOT EXISTS goods_timestamp_idx ON goods (timestamp DESC, height DESC);"
                            + "ALTER TABLE goods ADD COLUMN IF NOT EXISTS parsed_tags ARRAY;"
                            + "CREATE INDEX IF NOT EXISTS goods_height_id_idx ON goods (height, id);"
                            + "ALTER TABLE goods ADD COLUMN IF NOT EXISTS has_image BOOLEAN NOT NULL DEFAULT FALSE;"
                    );
                case 11:
                    apply("CREATE TABLE IF NOT EXISTS purchase (db_id IDENTITY, id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                            + "goods_id BIGINT NOT NULL, "
                            + "seller_id BIGINT NOT NULL, quantity INT NOT NULL, "
                            + "price BIGINT NOT NULL, deadline INT NOT NULL, note VARBINARY, nonce BINARY(32), "
                            + "timestamp INT NOT NULL, pending BOOLEAN NOT NULL, goods VARBINARY, goods_nonce BINARY(32), goods_is_text BOOLEAN NOT NULL DEFAULT TRUE, "
                            + "refund_note VARBINARY, refund_nonce BINARY(32), has_feedback_notes BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "has_public_feedbacks BOOLEAN NOT NULL DEFAULT FALSE, discount BIGINT NOT NULL, refund BIGINT NOT NULL, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS purchase_id_height_idx ON purchase (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS purchase_buyer_id_height_idx ON purchase (buyer_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS purchase_seller_id_height_idx ON purchase (seller_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS purchase_deadline_idx ON purchase (deadline DESC, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS purchase_timestamp_idx ON purchase (timestamp DESC, id);"
                            + "CREATE INDEX IF NOT EXISTS purchase_height_id_idx ON purchase (height, id);"
                            + "ALTER TABLE purchase ADD COLUMN IF NOT EXISTS goods_is_text BOOLEAN NOT NULL DEFAULT TRUE;"
                    );
                case 12:
                    apply("CREATE TABLE IF NOT EXISTS account (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "balance BIGINT NOT NULL, unconfirmed_balance BIGINT NOT NULL, "
                            + "forged_balance BIGINT NOT NULL, active_lessee_id BIGINT, has_control_phasing BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON account (id, height DESC);"
                            + "ALTER TABLE account ADD COLUMN IF NOT EXISTS active_lessee_id BIGINT;"
                            + "CREATE INDEX IF NOT EXISTS account_active_lessee_id_idx ON account (active_lessee_id);"
                            + "CREATE INDEX IF NOT EXISTS account_height_id_idx ON account (height, id);"
                            + "ALTER TABLE account DROP COLUMN IF EXISTS creation_height;"
                            + "ALTER TABLE account DROP COLUMN IF EXISTS key_height;"
                            + "ALTER TABLE account DROP COLUMN IF EXISTS public_key;"
                            + "ALTER TABLE account ADD COLUMN IF NOT EXISTS has_control_phasing BOOLEAN NOT NULL DEFAULT FALSE;"
                            + "ALTER TABLE account ADD COLUMN IF NOT EXISTS frozen_balance BIGINT NOT NULL DEFAULT 0;"
                    );
                case 13:
                    apply("CREATE TABLE IF NOT EXISTS account_asset (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "asset_id BIGINT NOT NULL, quantity BIGINT NOT NULL, unconfirmed_quantity BIGINT NOT NULL, height INT NOT NULL, "
                            + "latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_asset_quantity_idx ON account_asset (quantity DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_asset_asset_id_idx ON account_asset (asset_id);"
                            + "CREATE INDEX IF NOT EXISTS account_asset_height_id_idx ON account_asset (height, account_id, asset_id);"
                    );
                case 14:
                    apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "additions BIGINT NOT NULL, height INT NOT NULL);"
                            + "CREATE INDEX IF NOT EXISTS account_guaranteed_balance_height_idx ON account_guaranteed_balance(height);"
                    );
                case 15:
                    apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
                            + "(account_id, height DESC)");
                case 16:
                    apply("CREATE TABLE IF NOT EXISTS purchase_feedback (db_id IDENTITY, id BIGINT NOT NULL, feedback_data VARBINARY NOT NULL, "
                            + "feedback_nonce BINARY(32) NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE INDEX IF NOT EXISTS purchase_feedback_id_height_idx ON purchase_feedback (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS purchase_feedback_height_id_idx ON purchase_feedback (height, id);"
                    );
                case 17:
                    apply("CREATE TABLE IF NOT EXISTS purchase_public_feedback (db_id IDENTITY, id BIGINT NOT NULL, public_feedback "
                            + "VARCHAR NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE INDEX IF NOT EXISTS purchase_public_feedback_id_height_idx ON purchase_public_feedback (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS purchase_public_feedback_height_id_idx ON purchase_public_feedback (height, id);"
                    );
                case 18:
                    apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (db_id IDENTITY, id BIGINT NOT NULL, expiration INT NOT NULL, "
                            + "transaction_height INT NOT NULL, fee_per_byte BIGINT NOT NULL, arrival_timestamp BIGINT NOT NULL, "
                            + "transaction_bytes VARBINARY NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_idx ON unconfirmed_transaction (id);"
                            + "ALTER TABLE unconfirmed_transaction DROP COLUMN IF EXISTS timestamp;"
                            + "ALTER TABLE unconfirmed_transaction ADD COLUMN IF NOT EXISTS arrival_timestamp BIGINT NOT NULL DEFAULT 0;"
                            + "CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction (transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC);"
                            + "ALTER TABLE unconfirmed_transaction ADD COLUMN IF NOT EXISTS prunable_json VARCHAR;"
                            + "CREATE INDEX IF NOT EXISTS unconfirmed_transaction_expiration_idx ON unconfirmed_transaction (expiration DESC);"
                    );
                case 19:
                    apply("CREATE TABLE IF NOT EXISTS asset_transfer (db_id IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
                            + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, quantity BIGINT NOT NULL, timestamp INT NOT NULL, "
                            + "height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS asset_transfer_id_idx ON asset_transfer (id);"
                            + "CREATE INDEX IF NOT EXISTS asset_transfer_asset_id_idx ON asset_transfer (asset_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_transfer_sender_id_idx ON asset_transfer (sender_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_transfer_recipient_id_idx ON asset_transfer (recipient_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_transfer_height_idx ON asset_transfer(height);"
                    );
                case 20:
                    apply("CREATE TABLE IF NOT EXISTS tag (db_id IDENTITY, tag VARCHAR NOT NULL, in_stock_count INT NOT NULL, "
                            + "total_count INT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS tag_tag_idx ON tag (tag, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS tag_in_stock_count_idx ON tag (in_stock_count DESC, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS tag_height_tag_idx ON tag (height, tag);"
                    );
                case 21:
                    apply("CREATE TABLE IF NOT EXISTS currency (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "name VARCHAR NOT NULL, name_lower VARCHAR AS LOWER (name) NOT NULL, code VARCHAR NOT NULL, "
                            + "description VARCHAR, type INT NOT NULL, initial_supply BIGINT NOT NULL DEFAULT 0, "
                            + "reserve_supply BIGINT NOT NULL, max_supply BIGINT NOT NULL, creation_height INT NOT NULL, issuance_height INT NOT NULL, "
                            + "min_reserve_per_unit_nqt BIGINT NOT NULL, min_difficulty TINYINT NOT NULL, "
                            + "max_difficulty TINYINT NOT NULL, ruleset TINYINT NOT NULL, algorithm TINYINT NOT NULL, "
                            + "decimals TINYINT NOT NULL DEFAULT 0,"
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS currency_id_height_idx ON currency (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_account_id_idx ON currency (account_id);"
                            + "CREATE INDEX IF NOT EXISTS currency_name_idx ON currency (name_lower, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_code_idx ON currency (code, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_creation_height_idx ON currency (creation_height DESC);"
                            + "ALTER TABLE currency DROP COLUMN IF EXISTS current_supply;"
                            + "ALTER TABLE currency DROP COLUMN IF EXISTS current_reserve_per_unit_nqt;"
                            + "CREATE INDEX IF NOT EXISTS currency_issuance_height_idx ON currency (issuance_height);"
                            + "CREATE INDEX IF NOT EXISTS currency_height_id_idx ON currency (height, id);"
                    );
                case 22:
                    apply("CREATE TABLE IF NOT EXISTS account_currency (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "currency_id BIGINT NOT NULL, units BIGINT NOT NULL, unconfirmed_units BIGINT NOT NULL, height INT NOT NULL, "
                            + "latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_currency_id_height_idx ON account_currency (account_id, currency_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_currency_units_idx ON account_currency (units DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_currency_currency_id_idx ON account_currency (currency_id);"
                            + "CREATE INDEX IF NOT EXISTS account_currency_height_id_idx ON account_currency (height, account_id, currency_id);"
                    );
                case 23:
                    apply("CREATE TABLE IF NOT EXISTS currency_founder (db_id IDENTITY, currency_id BIGINT NOT NULL, "
                            + "account_id BIGINT NOT NULL, amount BIGINT NOT NULL, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS currency_founder_currency_id_idx ON currency_founder (currency_id, account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_founder_account_id_idx ON currency_founder (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_founder_height_id_idx ON currency_founder (height, currency_id, account_id);"
                    );
                case 24:
                    apply("CREATE TABLE IF NOT EXISTS currency_mint (db_id IDENTITY, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "counter BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS currency_mint_currency_id_account_id_idx ON currency_mint (currency_id, account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_mint_height_id_idx ON currency_mint (height, currency_id, account_id);"
                    );
                case 25:
                    apply("CREATE TABLE IF NOT EXISTS buy_offer (db_id IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL,"
                            + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL, transaction_height INT NOT NULL, "
                            + "creation_height INT NOT NULL, transaction_index SMALLINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS buy_offer_id_idx ON buy_offer (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS buy_offer_currency_id_account_id_idx ON buy_offer (currency_id, account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS buy_offer_rate_height_idx ON buy_offer (rate DESC, creation_height ASC);"
                            + "ALTER TABLE buy_offer ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS buy_offer_height_id_idx ON buy_offer (height, id);"
                    );
                case 26:
                    apply("CREATE TABLE IF NOT EXISTS sell_offer (db_id IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL, transaction_height INT NOT NULL, "
                            + "creation_height INT NOT NULL, transaction_index SMALLINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS sell_offer_id_idx ON sell_offer (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS sell_offer_currency_id_account_id_idx ON sell_offer (currency_id, account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS sell_offer_rate_height_idx ON sell_offer (rate ASC, creation_height ASC);"
                            + "ALTER TABLE sell_offer ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS sell_offer_height_id_idx ON sell_offer (height, id);"
                    );
                case 27:
                    apply("CREATE TABLE IF NOT EXISTS exchange (db_id IDENTITY, transaction_id BIGINT NOT NULL, currency_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                            + "offer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                            + "buyer_id BIGINT NOT NULL, units BIGINT NOT NULL, "
                            + "rate BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS exchange_offer_idx ON exchange (transaction_id, offer_id);"
                            + "CREATE INDEX IF NOT EXISTS exchange_currency_id_idx ON exchange (currency_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS exchange_seller_id_idx ON exchange (seller_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS exchange_buyer_id_idx ON exchange (buyer_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS exchange_height_idx ON exchange(height);"
                            + "CREATE INDEX IF NOT EXISTS exchange_height_db_id_idx ON exchange (height DESC, db_id DESC);"
                    );
                case 28:
                    apply("CREATE TABLE IF NOT EXISTS currency_transfer (db_id IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, "
                            + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, units BIGINT NOT NULL, timestamp INT NOT NULL, "
                            + "height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS currency_transfer_id_idx ON currency_transfer (id);"
                            + "CREATE INDEX IF NOT EXISTS currency_transfer_currency_id_idx ON currency_transfer (currency_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_transfer_sender_id_idx ON currency_transfer (sender_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_transfer_recipient_id_idx ON currency_transfer (recipient_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_transfer_height_idx ON currency_transfer(height);"
                    );
                case 29:
                    apply("CREATE TABLE IF NOT EXISTS scan (rescan BOOLEAN NOT NULL DEFAULT FALSE, height INT NOT NULL DEFAULT 0, "
                            + "validate BOOLEAN NOT NULL DEFAULT FALSE);"
                            + "INSERT INTO scan (rescan, height, validate) VALUES (false, 0, false);"
                    );
                case 30:
                    apply("CREATE TABLE IF NOT EXISTS currency_supply (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "current_supply BIGINT NOT NULL, current_reserve_per_unit_nqt BIGINT NOT NULL, height INT NOT NULL, "
                            + "latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS currency_supply_id_height_idx ON currency_supply (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS currency_supply_height_id_idx ON currency_supply (height, id);"
                    );
                case 31:
                    apply("CREATE TABLE IF NOT EXISTS public_key (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "public_key BINARY(32), height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, "
                            + "latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "ALTER TABLE public_key ADD COLUMN IF NOT EXISTS latest BOOLEAN NOT NULL DEFAULT TRUE;"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS public_key_account_id_height_idx ON public_key (account_id, height DESC);"
                    );
                case 32:
                    apply("CREATE TABLE IF NOT EXISTS vote (db_id IDENTITY, id BIGINT NOT NULL, " +
                            "poll_id BIGINT NOT NULL, voter_id BIGINT NOT NULL, vote_bytes VARBINARY NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS vote_id_idx ON vote (id);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS vote_poll_id_idx ON vote (poll_id, voter_id);"
                            + "CREATE INDEX IF NOT EXISTS vote_height_idx ON vote(height);"
                    );
                case 33:
                    apply("CREATE TABLE IF NOT EXISTS poll (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "account_id BIGINT NOT NULL, name VARCHAR NOT NULL, "
                            + "description VARCHAR, options ARRAY NOT NULL, min_num_options TINYINT, max_num_options TINYINT, "
                            + "min_range_value TINYINT, max_range_value TINYINT, timestamp INT NOT NULL, "
                            + "finish_height INT NOT NULL, voting_model TINYINT NOT NULL, min_balance BIGINT, "
                            + "min_balance_model TINYINT, holding_id BIGINT, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS poll_id_idx ON poll(id);"
                            + "CREATE INDEX IF NOT EXISTS poll_height_idx ON poll(height);"
                            + "CREATE INDEX IF NOT EXISTS poll_account_idx ON poll(account_id);"
                            + "CREATE INDEX IF NOT EXISTS poll_finish_height_idx ON poll(finish_height DESC);"
                            + "ALTER TABLE poll ADD COLUMN IF NOT EXISTS timestamp INT NOT NULL;"
                    );
                case 34:
                    apply("CREATE TABLE IF NOT EXISTS poll_result (db_id IDENTITY, poll_id BIGINT NOT NULL, "
                            + "result BIGINT, weight BIGINT NOT NULL, height INT NOT NULL);"
                            + "CREATE INDEX IF NOT EXISTS poll_result_poll_id_idx ON poll_result(poll_id);"
                            + "CREATE INDEX IF NOT EXISTS poll_result_height_idx ON poll_result(height);"

                    );
                case 35:
                    apply("CREATE TABLE IF NOT EXISTS phasing_poll (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "account_id BIGINT NOT NULL, whitelist_size TINYINT NOT NULL DEFAULT 0, "
                            + "finish_height INT NOT NULL, voting_model TINYINT NOT NULL, quorum BIGINT, "
                            + "min_balance BIGINT, holding_id BIGINT, min_balance_model TINYINT, "
                            + "hashed_secret VARBINARY, algorithm TINYINT, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_id_idx ON phasing_poll(id);"
                            + "CREATE INDEX IF NOT EXISTS phasing_poll_height_idx ON phasing_poll(height);"
                            + "CREATE INDEX IF NOT EXISTS phasing_poll_account_id_idx ON phasing_poll(account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS phasing_poll_holding_id_idx ON phasing_poll(holding_id, height DESC);"
                            + "ALTER TABLE phasing_poll DROP COLUMN IF EXISTS linked_full_hashes;"
                    );
                case 36:
                    apply("CREATE TABLE IF NOT EXISTS phasing_vote (db_id IDENTITY, vote_id BIGINT NOT NULL, "
                            + "transaction_id BIGINT NOT NULL, voter_id BIGINT NOT NULL, "
                            + "height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS phasing_vote_transaction_voter_idx ON phasing_vote(transaction_id, voter_id);"
                            + "CREATE INDEX IF NOT EXISTS phasing_vote_height_idx ON phasing_vote(height);"
                    );
                case 37:
                    apply("CREATE TABLE IF NOT EXISTS phasing_poll_voter (db_id IDENTITY, "
                            + "transaction_id BIGINT NOT NULL, voter_id BIGINT NOT NULL, "
                            + "height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_voter_transaction_voter_idx ON phasing_poll_voter(transaction_id, voter_id);"
                            + "CREATE INDEX IF NOT EXISTS phasing_poll_voter_height_idx ON phasing_poll_voter(height);"
                    );
                case 38:
                    apply("CREATE TABLE IF NOT EXISTS phasing_poll_result (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "result BIGINT NOT NULL, approved BOOLEAN NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_result_id_idx ON phasing_poll_result(id);"
                            + "CREATE INDEX IF NOT EXISTS phasing_poll_result_height_idx ON phasing_poll_result(height);"

                    );
                case 39:
                    apply("CREATE TABLE IF NOT EXISTS account_info (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "name VARCHAR, description VARCHAR, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_info_id_height_idx ON account_info (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_info_height_id_idx ON account_info (height, account_id);"
                    );
                case 40:
                    apply("CREATE TABLE IF NOT EXISTS prunable_message (db_id IDENTITY, id BIGINT NOT NULL, sender_id BIGINT NOT NULL, "
                            + "recipient_id BIGINT, message VARBINARY NOT NULL, is_text BOOLEAN NOT NULL, is_compressed BOOLEAN NOT NULL, "
                            + "encrypted_message VARBINARY, encrypted_is_text BOOLEAN DEFAULT FALSE, "
                            + "is_encrypted BOOLEAN NOT NULL, timestamp INT NOT NULL, expiration INT NOT NULL, height INT NOT NULL, "
                            + "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS prunable_message_id_idx ON prunable_message (id);"
                            + "CREATE INDEX IF NOT EXISTS prunable_message_transaction_timestamp_idx ON prunable_message (expiration DESC);"
                            + "CREATE INDEX IF NOT EXISTS prunable_message_sender_idx ON prunable_message (sender_id);"
                            + "CREATE INDEX IF NOT EXISTS prunable_message_recipient_idx ON prunable_message (recipient_id);"
                            + "ALTER TABLE prunable_message ALTER COLUMN expiration RENAME TO transaction_timestamp;"
                            + "ALTER TABLE prunable_message ALTER COLUMN timestamp RENAME TO block_timestamp;"
                            + "CREATE INDEX IF NOT EXISTS prunable_message_block_timestamp_dbid_idx ON prunable_message (block_timestamp DESC, db_id DESC);"
                            + "ALTER TABLE prunable_message ADD COLUMN IF NOT EXISTS encrypted_message VARBINARY;"
                            + "ALTER TABLE prunable_message ADD COLUMN IF NOT EXISTS encrypted_is_text BOOLEAN DEFAULT FALSE;"
                            + "ALTER TABLE prunable_message ALTER COLUMN message SET NULL;"
                            + "ALTER TABLE prunable_message ALTER COLUMN is_text RENAME TO message_is_text;"
                            + "ALTER TABLE prunable_message DROP COLUMN IF EXISTS is_encrypted;"

                    );
                case 41:
                    apply("CREATE TABLE IF NOT EXISTS tagged_data (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "name VARCHAR NOT NULL, description VARCHAR, tags VARCHAR, parsed_tags ARRAY, type VARCHAR, data VARBINARY NOT NULL, "
                            + "is_text BOOLEAN NOT NULL, filename VARCHAR, channel VARCHAR, block_timestamp INT NOT NULL, transaction_timestamp INT NOT NULL, "
                            + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_id_height_idx ON tagged_data (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_expiration_idx ON tagged_data (transaction_timestamp DESC);"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_account_id_height_idx ON tagged_data (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_block_timestamp_height_db_id_idx ON tagged_data (block_timestamp DESC, height DESC, db_id DESC);"
                            + "ALTER TABLE tagged_data ADD COLUMN IF NOT EXISTS channel VARCHAR;"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_channel_idx ON tagged_data (channel, height DESC);"
                    );
                case 42:
                    apply("CREATE TABLE IF NOT EXISTS data_tag (db_id IDENTITY, tag VARCHAR NOT NULL, tag_count INT NOT NULL, "
                            + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS data_tag_tag_height_idx ON data_tag (tag, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS data_tag_count_height_idx ON data_tag (tag_count DESC, height DESC);"
                    );
                case 43:
                    apply("CREATE TABLE IF NOT EXISTS tagged_data_timestamp (db_id IDENTITY, id BIGINT NOT NULL, timestamp INT NOT NULL, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_timestamp_id_height_idx ON tagged_data_timestamp (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_timestamp_height_id_idx ON tagged_data_timestamp (height, id);"
                    );
                case 44:
                    apply("CREATE TABLE IF NOT EXISTS account_lease (db_id IDENTITY, lessor_id BIGINT NOT NULL, "
                            + "current_leasing_height_from INT, current_leasing_height_to INT, current_lessee_id BIGINT, "
                            + "next_leasing_height_from INT, next_leasing_height_to INT, next_lessee_id BIGINT, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_lease_lessor_id_height_idx ON account_lease (lessor_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_lease_current_leasing_height_from_idx ON account_lease (current_leasing_height_from);"
                            + "CREATE INDEX IF NOT EXISTS account_lease_current_leasing_height_to_idx ON account_lease (current_leasing_height_to);"
                            + "CREATE INDEX IF NOT EXISTS account_lease_height_id_idx ON account_lease (height, lessor_id);"

                    );
                case 45:
                    apply("CREATE TABLE IF NOT EXISTS exchange_request (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "currency_id BIGINT NOT NULL, units BIGINT NOT NULL, rate BIGINT NOT NULL, is_buy BOOLEAN NOT NULL, "
                            + "timestamp INT NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS exchange_request_id_idx ON exchange_request (id);"
                            + "CREATE INDEX IF NOT EXISTS exchange_request_account_currency_idx ON exchange_request (account_id, currency_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS exchange_request_currency_idx ON exchange_request (currency_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS exchange_request_height_db_id_idx ON exchange_request (height DESC, db_id DESC);"
                            + "CREATE INDEX IF NOT EXISTS exchange_request_height_idx ON exchange_request (height);"
                    );
                case 46:
                    apply("CREATE TABLE IF NOT EXISTS account_ledger (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "event_type TINYINT NOT NULL, event_id BIGINT NOT NULL, holding_type TINYINT NOT NULL, "
                            + "holding_id BIGINT, change BIGINT NOT NULL, balance BIGINT NOT NULL, "
                            + "block_id BIGINT NOT NULL, height INT NOT NULL, timestamp INT NOT NULL);"
                            + "CREATE INDEX IF NOT EXISTS account_ledger_id_idx ON account_ledger(account_id, db_id);"
                            + "CREATE INDEX IF NOT EXISTS account_ledger_height_idx ON account_ledger(height);"

                    );
                case 47:
                    apply("CREATE TABLE IF NOT EXISTS tagged_data_extend (db_id IDENTITY, id BIGINT NOT NULL, "
                            + "extend_id BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_extend_id_height_idx ON tagged_data_extend(id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS tagged_data_extend_height_id_idx ON tagged_data_extend(height, id);"
                    );
                case 48:
                    FullTextTrigger.init();
                    apply(null);
                case 49:
                    apply("CREATE TABLE IF NOT EXISTS shuffling (db_id IDENTITY, id BIGINT NOT NULL, holding_id BIGINT NULL, holding_type TINYINT NOT NULL, "
                            + "issuer_id BIGINT NOT NULL, amount BIGINT NOT NULL, participant_count TINYINT NOT NULL, blocks_remaining SMALLINT NULL, "
                            + "stage TINYINT NOT NULL, assignee_account_id BIGINT NULL, registrant_count TINYINT NOT NULL, "
                            + "recipient_public_keys ARRAY, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS shuffling_id_height_idx ON shuffling (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS shuffling_holding_id_height_idx ON shuffling (holding_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS shuffling_assignee_account_id_height_idx ON shuffling (assignee_account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS shuffling_height_id_idx ON shuffling (height, id);"
                            + "ALTER TABLE shuffling ADD COLUMN IF NOT EXISTS registrant_count TINYINT NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS shuffling_blocks_remaining_height_idx ON shuffling (blocks_remaining, height DESC);"
                    );
                case 50:
                    apply("CREATE TABLE IF NOT EXISTS shuffling_participant (db_id IDENTITY, shuffling_id BIGINT NOT NULL, "
                            + "account_id BIGINT NOT NULL, next_account_id BIGINT NULL, participant_index TINYINT NOT NULL, "
                            + "state TINYINT NOT NULL, blame_data ARRAY, key_seeds ARRAY, data_transaction_full_hash BINARY(32), "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS shuffling_participant_shuffling_id_account_id_idx ON shuffling_participant (shuffling_id, account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS shuffling_participant_height_idx ON shuffling_participant (height, shuffling_id, account_id);"
                    );
                case 51:
                    apply("CREATE TABLE IF NOT EXISTS shuffling_data (db_id IDENTITY, shuffling_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                            + "data ARRAY, transaction_timestamp INT NOT NULL, height INT NOT NULL, "
                            + "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS shuffling_data_id_height_idx ON shuffling_data (shuffling_id, height DESC);"
                            + "CREATE INDEX shuffling_data_transaction_timestamp_idx ON shuffling_data (transaction_timestamp DESC);"
                    );
                case 52:
                    apply("CREATE TABLE IF NOT EXISTS phasing_poll_linked_transaction (db_id IDENTITY, "
                            + "transaction_id BIGINT NOT NULL, linked_full_hash BINARY(32) NOT NULL, linked_transaction_id BIGINT NOT NULL, "
                            + "height INT NOT NULL);"
                            + "CREATE INDEX IF NOT EXISTS phasing_poll_linked_transaction_height_idx ON phasing_poll_linked_transaction (height);"
                            + "CREATE " + (Constants.isTestnetOrDevnet() ? "" : "UNIQUE ") + "INDEX IF NOT EXISTS phasing_poll_linked_transaction_id_link_idx "
                            + "ON phasing_poll_linked_transaction (transaction_id, linked_transaction_id);"
                            + "CREATE " + (Constants.isTestnetOrDevnet() ? "" : "UNIQUE ") + "INDEX IF NOT EXISTS phasing_poll_linked_transaction_link_id_idx "
                            + "ON phasing_poll_linked_transaction (linked_transaction_id, transaction_id);"
                    );
                case 53:
                    apply("CREATE TABLE IF NOT EXISTS account_control_phasing (db_id IDENTITY, account_id BIGINT NOT NULL, "
                            + "whitelist ARRAY, voting_model TINYINT NOT NULL, quorum BIGINT, min_balance BIGINT, "
                            + "holding_id BIGINT, min_balance_model TINYINT, max_fees BIGINT, min_duration SMALLINT, max_duration SMALLINT, "
                            + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_control_phasing_id_height_idx ON account_control_phasing (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_control_phasing_height_id_idx ON account_control_phasing (height, account_id);"
                    );
                case 54:
                    apply("CREATE TABLE IF NOT EXISTS account_property (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, setter_id BIGINT, "
                            + "property VARCHAR NOT NULL, value VARCHAR, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_property_id_height_idx ON account_property (id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_property_height_id_idx ON account_property (height, id);"
                            + "CREATE INDEX IF NOT EXISTS account_property_recipient_height_idx ON account_property (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS account_property_setter_recipient_idx ON account_property (setter_id, account_id);"
                            + "ALTER TABLE account_property ALTER COLUMN account_id RENAME TO recipient_id;"
                    );
                case 55:
                    apply("CREATE TABLE IF NOT EXISTS asset_delete (db_id IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
                            + "account_id BIGINT NOT NULL, quantity BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS asset_delete_id_idx ON asset_delete (id);"
                            + "CREATE INDEX IF NOT EXISTS asset_delete_asset_id_idx ON asset_delete (asset_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_delete_account_id_idx ON asset_delete (account_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_delete_height_idx ON asset_delete (height);"
                    );
                case 56:
                    apply("CREATE TABLE IF NOT EXISTS referenced_transaction (db_id IDENTITY, transaction_id BIGINT NOT NULL, "
                            + "FOREIGN KEY (transaction_id) REFERENCES transaction (id) ON DELETE CASCADE, "
                            + "referenced_transaction_id BIGINT NOT NULL);"
                            + "CREATE INDEX IF NOT EXISTS referenced_transaction_referenced_transaction_id_idx ON referenced_transaction (referenced_transaction_id);"
                    );
                case 57:
                    try {
                        PreparedStatement pstmt = con.prepareStatement(
                                "SELECT id, referenced_transaction_full_hash FROM transaction WHERE referenced_transaction_full_hash IS NOT NULL");
                        PreparedStatement pstmtInsert = con.prepareStatement(
                                "INSERT INTO referenced_transaction (transaction_id, referenced_transaction_id) VALUES (?, ?)");
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            pstmtInsert.setLong(1, rs.getLong("id"));
                            pstmtInsert.setLong(2, Convert.fullHashToId(rs.getBytes("referenced_transaction_full_hash")));
                            pstmtInsert.executeUpdate();
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                    apply(null);
                case 58:
                    try {
                        Statement stmt = con.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS "
                                + "WHERE TABLE_NAME='BLOCK' AND (COLUMN_LIST='NEXT_BLOCK_ID' OR COLUMN_LIST='PREVIOUS_BLOCK_ID')");
                        List<String> constraintNames = new ArrayList<>();
                        while (rs.next()) {
                            constraintNames.add(rs.getString(1));
                        }
                        for (String constraintName : constraintNames) {
                            stmt.executeUpdate("ALTER TABLE BLOCK DROP CONSTRAINT " + constraintName);
                        }
                        apply(null);
                    } catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                case 59:
                    apply("CREATE TABLE IF NOT EXISTS account_fxt (id BIGINT NOT NULL, balance VARBINARY NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS account_fxt_id_idx ON account_fxt (id, height DESC);"
                    );
                case 60:
                    apply("CREATE TABLE IF NOT EXISTS asset_dividend (db_id IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
                            + "amount BIGINT NOT NULL, dividend_height INT NOT NULL, total_dividend BIGINT NOT NULL, "
                            + "num_accounts BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL);"
                            + "CREATE UNIQUE INDEX IF NOT EXISTS asset_dividend_id_idx ON asset_dividend (id);"
                            + "CREATE INDEX IF NOT EXISTS asset_dividend_asset_id_idx ON asset_dividend (asset_id, height DESC);"
                            + "CREATE INDEX IF NOT EXISTS asset_dividend_height_idx ON asset_dividend (height);"
                    );
                case 61:
                    apply("CREATE TABLE IF NOT EXISTS storage_backup (db_id IDENTITY, storer_id BIGINT NOT NULL, store_target VARCHAR," +
                            " store_transaction BIGINT NOT NULL, backup_transaction BIGINT NOT NULL, "
                            + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE)");
                case 62:
                    apply("CREATE TABLE IF NOT EXISTS account_poc_score (db_id IDENTITY, account_id BIGINT NOT NULL,"
                            + "poc_score BIGINT NOT NULL, height INT NOT NULL, poc_detail VARCHAR);"
                            + "CREATE INDEX IF NOT EXISTS account_height_idx ON account_poc_score (account_id, height DESC);"
                            + "ALTER TABLE account_poc_score ADD COLUMN IF NOT EXISTS latest BOOLEAN NOT NULL DEFAULT TRUE;"
                    );
                case 63:
                    apply("CREATE TABLE IF NOT EXISTS account_pool (db_id IDENTITY, pool_id BIGINT NOT NULL, creator_id BIGINT NOT NULL,"
                            + "state TINYINT NOT NULL, pool_detail VARCHAR);"
                            + "ALTER TABLE account_pool ADD COLUMN IF NOT EXISTS height INT NOT NULL;"
                            + "CREATE INDEX IF NOT EXISTS pool_id_height_idx ON account_pool (pool_id, height DESC);"
                    );
                case 64:
                    apply("CREATE TABLE IF NOT EXISTS certified_peer (db_id IDENTITY, host VARCHAR NOT NULL, account_id BIGINT NOT NULL,"
                            + "type INT NOT NULL, height INT NOT NULL, last_updated INT, latest BOOLEAN NOT NULL DEFAULT TRUE);"
                            + "CREATE INDEX IF NOT EXISTS peer_account_height_idx ON certified_peer (account_id, height DESC);"
                    );
                case 65:
                    apply(
                            "alter table ACCOUNT rename to ACCOUNT_HISTORY;\n" +
                                    "alter index ACCOUNT_ID_HEIGHT_IDX rename to ACCOUNT_HISTORY_ID_HEIGHT_IDX;\n" +
                                    "alter index ACCOUNT_ACTIVE_LESSEE_ID_IDX rename to ACCOUNT_HISTORY_ACTIVE_LESSEE_ID_IDX;\n" +
                                    "alter index ACCOUNT_HEIGHT_ID_IDX rename to ACCOUNT_HISTORY_HEIGHT_ID_IDX;\n" +

                                    "alter table ACCOUNT_GUARANTEED_BALANCE rename to ACCOUNT_GUARANTEED_BALANCE_HISTORY;\n" +

                                    "alter table ACCOUNT_POC_SCORE rename to ACCOUNT_POC_SCORE_HISTORY;\n" +
                                    "alter index ACCOUNT_HEIGHT_IDX rename to ACCOUNT_POC_SCORE_HISTORY_HEIGHT_IDX;\n" +

                                    "create table IF NOT EXISTS ACCOUNT_CACHE\n" +
                                    "(\n" +
                                    "    DB_ID               BIGINT auto_increment,\n" +
                                    "    ID                  BIGINT                not null,\n" +
                                    "    BALANCE             BIGINT                not null,\n" +
                                    "    UNCONFIRMED_BALANCE BIGINT                not null,\n" +
                                    "    FORGED_BALANCE      BIGINT                not null,\n" +
                                    "    ACTIVE_LESSEE_ID    BIGINT,\n" +
                                    "    HAS_CONTROL_PHASING BOOLEAN default FALSE not null,\n" +
                                    "    HEIGHT              INT                   not null,\n" +
                                    "    LATEST              BOOLEAN default TRUE  not null,\n" +
                                    "    FROZEN_BALANCE      BIGINT  default 0     not null,\n" +
                                    "    primary key (DB_ID)\n" +
                                    ");\n" +

                                    "create table IF NOT EXISTS ACCOUNT\n" +
                                    "(\n" +
                                    "    DB_ID               BIGINT auto_increment,\n" +
                                    "    ID                  BIGINT                not null,\n" +
                                    "    BALANCE             BIGINT                not null,\n" +
                                    "    UNCONFIRMED_BALANCE BIGINT                not null,\n" +
                                    "    FORGED_BALANCE      BIGINT                not null,\n" +
                                    "    ACTIVE_LESSEE_ID    BIGINT,\n" +
                                    "    HAS_CONTROL_PHASING BOOLEAN default FALSE not null,\n" +
                                    "    HEIGHT              INT                   not null,\n" +
                                    "    LATEST              BOOLEAN default TRUE  not null,\n" +
                                    "    FROZEN_BALANCE      BIGINT  default 0     not null,\n" +
                                    "    primary key (DB_ID)\n" +
                                    ");\n" +
                                    "create table IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE_CACHE\n" +
                                    "(\n" +
                                    "    DB_ID      BIGINT auto_increment,\n" +
                                    "    ACCOUNT_ID BIGINT               not null,\n" +
                                    "    ADDITIONS  BIGINT               not null,\n" +
                                    "    HEIGHT     INT                  not null,\n" +
                                    "    LATEST     BOOLEAN default TRUE not null,\n" +
                                    "    primary key (DB_ID)\n" +
                                    ");\n" +

                                    "create table IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE\n" +
                                    "(\n" +
                                    "    DB_ID      BIGINT auto_increment,\n" +
                                    "    ACCOUNT_ID BIGINT               not null,\n" +
                                    "    ADDITIONS  BIGINT               not null,\n" +
                                    "    HEIGHT     INT                  not null,\n" +
                                    "    LATEST     BOOLEAN default TRUE not null,\n" +
                                    "    primary key (DB_ID)\n" +
                                    ");\n" +

                                    "create table IF NOT EXISTS ACCOUNT_POC_SCORE_CACHE\n" +
                                    "(\n" +
                                    "    DB_ID      BIGINT auto_increment,\n" +
                                    "    ACCOUNT_ID BIGINT               not null,\n" +
                                    "    POC_SCORE  BIGINT               not null,\n" +
                                    "    HEIGHT     INT                  not null,\n" +
                                    "    POC_DETAIL VARCHAR,\n" +
                                    "    LATEST     BOOLEAN default TRUE not null,\n" +
                                    "    primary key (DB_ID)\n" +
                                    ");\n" +

                                    "CREATE TABLE IF NOT EXISTS ACCOUNT_POC_SCORE\n" +
                                    "(\n" +
                                    "    DB_ID      BIGINT auto_increment,\n" +
                                    "    ACCOUNT_ID BIGINT               not null,\n" +
                                    "    POC_SCORE  BIGINT               not null,\n" +
                                    "    HEIGHT     INT                  not null,\n" +
                                    "    POC_DETAIL VARCHAR,\n" +
                                    "    LATEST     BOOLEAN default TRUE not null,\n" +
                                    "    primary key (DB_ID)\n" +
                                    ");\n" +
                                    "create unique index IF NOT EXISTS ACCOUNT_CACHE_ID_HEIGHT_IDX on ACCOUNT_CACHE (ID asc, HEIGHT desc);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_CACHE_ACTIVE_LESSEE_ID_IDX on ACCOUNT_CACHE (ACTIVE_LESSEE_ID);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_CACHE_HEIGHT_ID_IDX on ACCOUNT_CACHE (HEIGHT, ID);\n" +

                                    "create unique index IF NOT EXISTS ACCOUNT_ID_HEIGHT_IDX on ACCOUNT (ID asc, HEIGHT desc);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_ACTIVE_LESSEE_ID_IDX on ACCOUNT (ACTIVE_LESSEE_ID);\n" +

                                    "create unique index IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE_CACHE_ID_HEIGHT_IDX on ACCOUNT_GUARANTEED_BALANCE_CACHE (ACCOUNT_ID asc, HEIGHT desc);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE_CACHE_HEIGHT_IDX on ACCOUNT_GUARANTEED_BALANCE_CACHE (HEIGHT);\n" +

                                    "create unique index IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE_ID_HEIGHT_IDX on ACCOUNT_GUARANTEED_BALANCE (ACCOUNT_ID asc, HEIGHT desc);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE_HEIGHT_IDX on ACCOUNT_GUARANTEED_BALANCE (HEIGHT);\n" +

                                    "create index IF NOT EXISTS ACCOUNT_LEDGER_ID_IDX on ACCOUNT_LEDGER (ACCOUNT_ID, DB_ID);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_LEDGER_HEIGHT_IDX on ACCOUNT_LEDGER (HEIGHT);\n" +

                                    "create index IF NOT EXISTS ACCOUNT_HEIGHT_IDX on ACCOUNT_POC_SCORE (ACCOUNT_ID asc, HEIGHT desc);\n" +
                                    "create index IF NOT EXISTS ACCOUNT_POC_SCORE_CACHE_IDX on ACCOUNT_POC_SCORE_CACHE (ACCOUNT_ID asc, HEIGHT desc);\n" +

                                    "create index IF NOT EXISTS ACCOUNT_POC_SCORE_HEIGHT_INDEX on ACCOUNT_POC_SCORE (HEIGHT desc);\n"
                                    + "create index IF NOT EXISTS ACCOUNT_POC_SCORE_CACHE_HEIGHT_INDEX on ACCOUNT_POC_SCORE_CACHE (HEIGHT desc);"
                                    + "CREATE INDEX IF NOT EXISTS ACCOUNT_HEIGHT_INDEX ON ACCOUNT (HEIGHT DESC);\n"
                                    + "CREATE INDEX IF NOT EXISTS ACCOUNT_POC_SCORE_HEIGHT_INDEX ON ACCOUNT_POC_SCORE (HEIGHT DESC);\n"
                    );
                case 66:
                    int maxDistributeHeight = 0;
                    try {
                        PreparedStatement pstmt = con.prepareStatement(
                                "SELECT height FROM block where HAS_REWARD_DISTRIBUTION = true order by height desc limit 1");
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            maxDistributeHeight = rs.getInt("height");
                        }
                        apply("alter table BLOCK alter column HAS_REWARD_DISTRIBUTION rename to REWARD_DISTRIBUTION_HEIGHT;\n" +
                                "alter table BLOCK alter column REWARD_DISTRIBUTION_HEIGHT int default 0 not null;");
                        PreparedStatement currentHeightpstmt = con.prepareStatement(
                                "SELECT height FROM block order by height desc limit 1");
                        PreparedStatement pstmtUpdate = con.prepareStatement(
                                "update block set REWARD_DISTRIBUTION_HEIGHT = ? where height <= ? and height > ?");
                        ResultSet heightRs = currentHeightpstmt.executeQuery();
                        // original settlement interval size
                        int settlementIntervalSize = 432;
                        while (heightRs.next()) {
                            int height = heightRs.getInt("height");
                            int i = height / settlementIntervalSize;
                            if (i > 0) {
                                for (int j = 1; j <= i; j++) {
                                    int rewardDistributionHeight = j * settlementIntervalSize;
                                    if (rewardDistributionHeight > 6048) {
                                        break;
                                    }
                                    if (rewardDistributionHeight <= maxDistributeHeight) {
                                        int latestRewardDistributionHeight = (j - 1) * settlementIntervalSize;
                                        if (rewardDistributionHeight <= 5184) {
                                            pstmtUpdate.setInt(1, rewardDistributionHeight);
                                        } else if (rewardDistributionHeight > 5184 && rewardDistributionHeight <= 6048) {
                                            pstmtUpdate.setInt(1, 6048);
                                        }
                                        pstmtUpdate.setInt(2, rewardDistributionHeight);
                                        pstmtUpdate.setInt(3, latestRewardDistributionHeight);
                                        pstmtUpdate.executeUpdate();
                                    }
                                }
                            }
                            if (height > 6048) {
                                settlementIntervalSize = 1008;
                                int intervalNum = height / settlementIntervalSize;
                                if (intervalNum > 6) {
                                    for (int j = 7; j <= intervalNum; j++) {
                                        int rewardDistributionHeight = j * settlementIntervalSize;
                                        if (rewardDistributionHeight <= maxDistributeHeight) {
                                            int latestRewardDistributionHeight = (j - 1) * settlementIntervalSize;
                                            pstmtUpdate.setInt(1, rewardDistributionHeight);
                                            pstmtUpdate.setInt(2, rewardDistributionHeight);
                                            pstmtUpdate.setInt(3, latestRewardDistributionHeight);
                                            pstmtUpdate.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                case 67:
                    apply("ALTER TABLE ACCOUNT_GUARANTEED_BALANCE_HISTORY ADD COLUMN IF NOT EXISTS LATEST BOOLEAN default false NOT NULL");
                case 68:
                    apply("alter table CERTIFIED_PEER add delete_Height int(10) not null default 0;");
                case 69:
                    apply("CREATE TABLE IF NOT EXISTS fork_block (\n" +
                            "    DB_ID                 BIGINT auto_increment\n" +
                            "        primary key,\n" +
                            "    ID                    BIGINT    not null,\n" +
                            "    VERSION               INT       not null,\n" +
                            "    TIMESTAMP             INT       not null,\n" +
                            "    PREVIOUS_BLOCK_ID     BIGINT,\n" +
                            "    CUMULATIVE_DIFFICULTY VARBINARY not null,\n" +
                            "    NEXT_BLOCK_ID         BIGINT,\n" +
                            "    HEIGHT                INT       not null,\n" +
                            "    GENERATOR_ID          BIGINT    not null\n" +
                            ");\n" +
                            "\n" +
                            "create unique index IF NOT EXISTS FORK_BLOCK_ID_IDX\n" +
                            "    on FORK_BLOCK (ID);" +
                            "CREATE TABLE IF NOT EXISTS fork_block_linked_account\n" +
                            "(\n" +
                            "    DB_ID      BIGINT auto_increment\n" +
                            "        primary key,\n" +
                            "    BLOCK_ID   BIGINT not null,\n" +
                            "    ACCOUNT_ID BIGINT not null,\n" +
                            "    HEIGHT     INT    not null\n" +
                            ");\n" +
                            "create unique index IF NOT EXISTS FORK_BLOCK_LINKED_ACCOUNT_ACCOUNT_ID_BLOCK_ID_UINDEX\n" +
                            "    on FORK_BLOCK_LINKED_ACCOUNT (ACCOUNT_ID, BLOCK_ID);"
                    );
                case 70:
                    break;
                default:
                    throw new RuntimeException("Blockchain database inconsistent with code, at update " + nextUpdate
                            + ", probably trying to run older code on newer database[ you can check the code in ConchDbVersion.java firstly]");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }
}
