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

package org.conch.asset.token;

import org.conch.asset.MonetaryTx;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.crypto.HashFunction;
import org.conch.mint.CurrencyMinting;
import org.conch.shuffle.ShufflingTransaction;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;

import java.util.EnumSet;
import java.util.Set;

/**
 * Define and validate currency capabilities
 */
public enum CurrencyType {

    /**
     * Can be exchanged from/to MWFS<br>
     */
    EXCHANGEABLE(0x01) {

        @Override
        void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
        }

        @Override
        void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
            if (transaction.getType() == MonetaryTx.CURRENCY_ISSUANCE) {
                if (!validators.contains(CLAIMABLE)) {
                    throw new ConchException.NotValidException("Currency is not exchangeable and not claimable");
                }
            }
            if (transaction.getType() instanceof MonetaryTx.MonetarySystemExchange || transaction.getType() == MonetaryTx.PUBLISH_EXCHANGE_OFFER) {
                throw new ConchException.NotValidException("Currency is not exchangeable");
            }
        }
    },
    /**
     * Transfers are only allowed from/to issuer account<br>
     * Only issuer account can publish exchange offer<br>
     */
    CONTROLLABLE(0x02) {

        @Override
        void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
            if (transaction.getType() == MonetaryTx.CURRENCY_TRANSFER) {
                if (currency == null ||  (currency.getAccountId() != transaction.getSenderId() && currency.getAccountId() != transaction.getRecipientId())) {
                    throw new ConchException.NotValidException("Controllable currency can only be transferred to/from issuer account");
                }
            }
            if (transaction.getType() == MonetaryTx.PUBLISH_EXCHANGE_OFFER) {
                if (currency == null || currency.getAccountId() != transaction.getSenderId()) {
                    throw new ConchException.NotValidException("Only currency issuer can publish an exchange offer for controllable currency");
                }
            }
        }

        @Override
        void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) {}

    },
    /**
     * Can be reserved before the currency is active, reserve is distributed to founders once the currency becomes active<br>
     */
    RESERVABLE(0x04) {

        @Override
        void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.ValidationException {
            if (transaction.getType() == MonetaryTx.CURRENCY_ISSUANCE) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
                int issuanceHeight = attachment.getIssuanceHeight();
                int finishHeight = attachment.getFinishValidationHeight(transaction);
                if  (issuanceHeight <= finishHeight) {
                    throw new ConchException.NotCurrentlyValidException(
                        String.format("Reservable currency activation height %d not higher than transaction apply height %d",
                                issuanceHeight, finishHeight));
                }
                if (attachment.getMinReservePerUnitNQT() <= 0) {
                    throw new ConchException.NotValidException("Minimum reserve per unit must be > 0");
                }
                if (Math.multiplyExact(attachment.getMinReservePerUnitNQT(), attachment.getReserveSupply()) > Constants.MAX_BALANCE_NQT) {
                    throw new ConchException.NotValidException("Minimum reserve per unit is too large");
                }
                if (attachment.getReserveSupply() <= attachment.getInitialSupply()) {
                    throw new ConchException.NotValidException("Reserve supply must exceed initial supply");
                }
                if (!validators.contains(MINTABLE) && attachment.getReserveSupply() < attachment.getMaxSupply()) {
                    throw new ConchException.NotValidException("Max supply must not exceed reserve supply for reservable and non-mintable currency");
                }
            }
            if (transaction.getType() == MonetaryTx.RESERVE_INCREASE) {
                Attachment.MonetarySystemReserveIncrease attachment = (Attachment.MonetarySystemReserveIncrease) transaction.getAttachment();
                if (currency != null && currency.getIssuanceHeight() <= attachment.getFinishValidationHeight(transaction)) {
                    throw new ConchException.NotCurrentlyValidException("Cannot increase reserve for active currency");
                }
            }
        }

        @Override
        void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
            if (transaction.getType() == MonetaryTx.RESERVE_INCREASE) {
                throw new ConchException.NotValidException("Cannot increase reserve since currency is not reservable");
            }
            if (transaction.getType() == MonetaryTx.CURRENCY_ISSUANCE) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
                if (attachment.getIssuanceHeight() != 0) {
                    throw new ConchException.NotValidException("Issuance height for non-reservable currency must be 0");
                }
                if (attachment.getMinReservePerUnitNQT() > 0) {
                    throw new ConchException.NotValidException("Minimum reserve per unit for non-reservable currency must be 0 ");
                }
                if (attachment.getReserveSupply() > 0) {
                    throw new ConchException.NotValidException("Reserve supply for non-reservable currency must be 0");
                }
                if (!validators.contains(MINTABLE) && attachment.getInitialSupply() < attachment.getMaxSupply()) {
                    throw new ConchException.NotValidException("Initial supply for non-reservable and non-mintable currency must be equal to max supply");
                }
            }
        }
    },
    /**
     * Is {@link #RESERVABLE} and can be claimed after currency is active<br>
     * Cannot be {@link #EXCHANGEABLE}
     */
    CLAIMABLE(0x08) {

        @Override
        void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.ValidationException {
            if (transaction.getType() == MonetaryTx.CURRENCY_ISSUANCE) {
                Attachment.MonetarySystemCurrencyIssuance attachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
                if (!validators.contains(RESERVABLE)) {
                    throw new ConchException.NotValidException("Claimable currency must be reservable");
                }
                if (validators.contains(MINTABLE)) {
                    throw new ConchException.NotValidException("Claimable currency cannot be mintable");
                }
                if (attachment.getInitialSupply() > 0) {
                    throw new ConchException.NotValidException("Claimable currency must have initial supply 0");
                }
            }
            if (transaction.getType() == MonetaryTx.RESERVE_CLAIM) {
                if (currency == null || !currency.isActive()) {
                    throw new ConchException.NotCurrentlyValidException("Cannot claim reserve since currency is not yet active");
                }
            }
        }

        @Override
        void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
            if (transaction.getType() == MonetaryTx.RESERVE_CLAIM) {
                throw new ConchException.NotValidException("Cannot claim reserve since currency is not claimable");
            }
        }
    },
    /**
     * Can be minted using proof of work algorithm<br>
     */
    MINTABLE(0x10) {
        @Override
        void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
            if (transaction.getType() == MonetaryTx.CURRENCY_ISSUANCE) {
                Attachment.MonetarySystemCurrencyIssuance issuanceAttachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
                try {
                    HashFunction hashFunction = HashFunction.getHashFunction(issuanceAttachment.getAlgorithm());
                    if (!CurrencyMinting.acceptedHashFunctions.contains(hashFunction)) {
                        throw new ConchException.NotValidException("Invalid minting algorithm " + hashFunction);
                    }
                } catch (IllegalArgumentException e) {
                    throw new ConchException.NotValidException("Illegal algorithm code specified" , e);
                }
                if (issuanceAttachment.getMinDifficulty() < 1 || issuanceAttachment.getMaxDifficulty() > 255 ||
                        issuanceAttachment.getMaxDifficulty() < issuanceAttachment.getMinDifficulty()) {
                    throw new ConchException.NotValidException(
                            String.format("Invalid minting difficulties min %d max %d, difficulty must be between 1 and 255, max larger than min",
                                    issuanceAttachment.getMinDifficulty(), issuanceAttachment.getMaxDifficulty()));
                }
                if (issuanceAttachment.getMaxSupply() <= issuanceAttachment.getReserveSupply()) {
                    throw new ConchException.NotValidException("Max supply for mintable currency must exceed reserve supply");
                }
            }
        }

        @Override
        void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.NotValidException {
            if (transaction.getType() == MonetaryTx.CURRENCY_ISSUANCE) {
                Attachment.MonetarySystemCurrencyIssuance issuanceAttachment = (Attachment.MonetarySystemCurrencyIssuance) transaction.getAttachment();
                if (issuanceAttachment.getMinDifficulty() != 0 ||
                        issuanceAttachment.getMaxDifficulty() != 0 ||
                        issuanceAttachment.getAlgorithm() != 0) {
                    throw new ConchException.NotValidException("Non mintable currency should not specify algorithm or difficulty");
                }
            }
            if (transaction.getType() == MonetaryTx.CURRENCY_MINTING) {
                throw new ConchException.NotValidException("Currency is not mintable");
            }
        }

    },
    /**
     * Several accounts can shuffle their currency units and then distributed to recipients<br>
     */
    NON_SHUFFLEABLE(0x20) {
        @Override
        void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.ValidationException {
            if (transaction.getType() == ShufflingTransaction.SHUFFLING_CREATION) {
                throw new ConchException.NotValidException("Shuffling is not allowed for this currency");
            }
        }

        @Override
        void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.ValidationException {
        }
    };

    private final int code;

    CurrencyType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    abstract void validate(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.ValidationException;

    abstract void validateMissing(Currency currency, Transaction transaction, Set<CurrencyType> validators) throws ConchException.ValidationException;

    public static CurrencyType get(int code) {
        for (CurrencyType currencyType : values()) {
            if (currencyType.getCode() == code) {
                return currencyType;
            }
        }
        return null;
    }

    public static void validate(Currency currency, Transaction transaction) throws ConchException.ValidationException {
        if (currency == null) {
            throw new ConchException.NotCurrentlyValidException("Unknown currency: " + transaction.getAttachment().getJSONObject());
        }
        validate(currency, currency.getType(), transaction);
    }

    public static void validate(int type, Transaction transaction) throws ConchException.ValidationException {
        validate(null, type, transaction);
    }

    private static void validate(Currency currency, int type, Transaction transaction) throws ConchException.ValidationException {
        if (transaction.getAmountNQT() != 0) {
            throw new ConchException.NotValidException("Currency transaction MWFS amount must be 0");
        }

        final EnumSet<CurrencyType> validators = EnumSet.noneOf(CurrencyType.class);
        for (CurrencyType validator : CurrencyType.values()) {
            if ((validator.getCode() & type) != 0) {
                validators.add(validator);
            }
        }
        if (validators.isEmpty()) {
            throw new ConchException.NotValidException("Currency type not specified");
        }
        for (CurrencyType validator : CurrencyType.values()) {
            if ((validator.getCode() & type) != 0) {
                validator.validate(currency, transaction, validators);
            } else {
                validator.validateMissing(currency, transaction, validators);
            }
        }
    }

    public static void validateCurrencyNaming(long issuerAccountId, Attachment.MonetarySystemCurrencyIssuance attachment) throws ConchException.ValidationException {
        String name = attachment.getName();
        String code = attachment.getCode();
        String description = attachment.getDescription();
        if (name.length() < Constants.MIN_CURRENCY_NAME_LENGTH || name.length() > Constants.MAX_CURRENCY_NAME_LENGTH
                || name.length() < code.length()
                || code.length() < Constants.MIN_CURRENCY_CODE_LENGTH || code.length() > Constants.MAX_CURRENCY_CODE_LENGTH
                || description.length() > Constants.MAX_CURRENCY_DESCRIPTION_LENGTH) {
            throw new ConchException.NotValidException(String.format("Invalid currency name %s code %s or description %s", name, code, description));
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                throw new ConchException.NotValidException("Invalid currency name: " + normalizedName);
            }
        }
        for (int i = 0; i < code.length(); i++) {
            if (Constants.ALLOWED_CURRENCY_CODE_LETTERS.indexOf(code.charAt(i)) < 0) {
                throw new ConchException.NotValidException("Invalid currency code: " + code + " code must be all upper case");
            }
        }
        if (code.contains("CDW")  || "cdw".equals(normalizedName) ) {
            throw new ConchException.NotValidException("Currency name already used");
        }
        Currency currency;
        if ((currency = Currency.getCurrencyByName(normalizedName)) != null && ! currency.canBeDeletedBy(issuerAccountId)) {
            throw new ConchException.NotCurrentlyValidException("Currency name already used: " + normalizedName);
        }
        if ((currency = Currency.getCurrencyByCode(name)) != null && ! currency.canBeDeletedBy(issuerAccountId)) {
            throw new ConchException.NotCurrentlyValidException("Currency name already used as code: " + normalizedName);
        }
        if ((currency = Currency.getCurrencyByCode(code)) != null && ! currency.canBeDeletedBy(issuerAccountId)) {
            throw new ConchException.NotCurrentlyValidException("Currency code already used: " + code);
        }
        if ((currency = Currency.getCurrencyByName(code)) != null && ! currency.canBeDeletedBy(issuerAccountId)) {
            throw new ConchException.NotCurrentlyValidException("Currency code already used as name: " + code);
        }
    }

}
