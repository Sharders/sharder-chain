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

import org.conch.*;
import org.conch.crypto.Crypto;
import org.conch.crypto.EncryptedData;
import org.conch.http.ParameterException;
import org.conch.http.biz.exception.BizParameterException;
import org.conch.storage.ipfs.IpfsService;
import org.conch.util.Convert;
import org.conch.util.Search;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;

import static org.conch.http.JSONResponses.*;
import static org.conch.http.JSONResponses.INCORRECT_DATA;
import static org.conch.http.JSONResponses.INCORRECT_TAGGED_DATA_FILENAME;

public class BizParameterParser  {

    public static String getSecretPhrase(HashMap<String, String> params, boolean isMandatory) throws BizParameterException {
        String secretPhrase = Convert.emptyToNull(params.get("secretPhrase"));
        if (secretPhrase == null && isMandatory) {
            throw new BizParameterException("secretPhrase not specified");
        }
        return secretPhrase;
    }

    public static Appendix getEncryptedMessage(HashMap<String, String> params, Account recipient, boolean prunable) throws BizParameterException {
        boolean isText = !"false".equalsIgnoreCase(params.get("messageToEncryptIsText"));
        boolean compress = !"false".equalsIgnoreCase(params.get("compressMessageToEncrypt"));
        byte[] plainMessageBytes = null;
        byte[] recipientPublicKey = null;
        EncryptedData encryptedData = BizParameterParser.getEncryptedData(params, "encryptedMessage");
        if (encryptedData == null) {
            String plainMessage = Convert.emptyToNull(params.get("messageToEncrypt"));
            try {
                plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            } catch (RuntimeException e) {
                throw new BizParameterException("messageToEncrypt not specified");
            }
            if (recipient != null) {
                recipientPublicKey = Account.getPublicKey(recipient.getId());
            }
            if (recipientPublicKey == null) {
                recipientPublicKey = Convert.parseHexString(Convert.emptyToNull(params.get("recipientPublicKey")));
            }
            if (recipientPublicKey == null) {
                throw new BizParameterException("recipientPublicKey not specified");
            }
            String secretPhrase = getSecretPhrase(params, false);
            if (secretPhrase != null) {
                encryptedData = Account.encryptTo(recipientPublicKey, plainMessageBytes, secretPhrase, compress);
            }
        }
        if (encryptedData != null) {
            if (prunable) {
                return new Appendix.PrunableEncryptedMessage(encryptedData, isText, compress);
            } else {
                return new Appendix.EncryptedMessage(encryptedData, isText, compress);
            }
        } else {
            if (prunable) {
                return new Appendix.UnencryptedPrunableEncryptedMessage(plainMessageBytes, isText, compress, recipientPublicKey);
            } else {
                return new Appendix.UnencryptedEncryptedMessage(plainMessageBytes, isText, compress, recipientPublicKey);
            }
        }
    }

    public static EncryptedData getEncryptedData(HashMap<String, String> params, String messageType) throws BizParameterException {
        String dataString = Convert.emptyToNull(params.get(messageType + "Data"));
        String nonceString = Convert.emptyToNull(params.get(messageType + "Nonce"));
        if (nonceString == null) {
            return null;
        }
        byte[] data;
        byte[] nonce;
        try {
            nonce = Convert.parseHexString(nonceString);
        } catch (RuntimeException e) {
            throw new BizParameterException("Incorrect" + messageType + "Nonce");
        }
        try {
            data = Convert.parseHexString(dataString);
        } catch (RuntimeException e) {
            throw new BizParameterException("Incorrect" + messageType + "Data");
        }
        return new EncryptedData(data, nonce);
    }

    public static Appendix getPlainMessage(HashMap<String, String> params, boolean prunable) throws BizParameterException {
        String messageValue = Convert.emptyToNull(params.get("message"));
        boolean messageIsText = !"false".equalsIgnoreCase(params.get("messageIsText"));
        try {
            if (prunable) {
                return new Appendix.PrunablePlainMessage(messageValue, messageIsText);
            } else {
                return new Appendix.Message(messageValue, messageIsText);
            }
        } catch (RuntimeException e) {
            throw new BizParameterException("param message wrong");
        }
    }

    public static Appendix.EncryptToSelfMessage getEncryptToSelfMessage(HashMap<String, String> params) throws BizParameterException {
        boolean isText = !"false".equalsIgnoreCase(params.get("messageToEncryptToSelfIsText"));
        boolean compress = !"false".equalsIgnoreCase(params.get("compressMessageToEncryptToSelf"));
        byte[] plainMessageBytes = null;
        EncryptedData encryptedData = BizParameterParser.getEncryptedData(params, "encryptToSelfMessage");
        if (encryptedData == null) {
            String plainMessage = Convert.emptyToNull(params.get("messageToEncryptToSelf"));
            if (plainMessage == null) {
                return null;
            }
            try {
                plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            } catch (RuntimeException e) {
                throw new BizParameterException("Incorrect messageToEncrypt");
            }
            String secretPhrase = getSecretPhrase(params, false);
            if (secretPhrase != null) {
                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                encryptedData = Account.encryptTo(publicKey, plainMessageBytes, secretPhrase, compress);
            }
        }
        if (encryptedData != null) {
            return new Appendix.EncryptToSelfMessage(encryptedData, isText, compress);
        } else {
            return new Appendix.UnencryptedEncryptToSelfMessage(plainMessageBytes, isText, compress);
        }
    }

    public static int getInt(HashMap<String, String> params, String name, int min, int max, boolean isMandatory) throws BizParameterException {
        String paramValue = Convert.emptyToNull(params.get(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new BizParameterException(name + " not specified");
            }
            return 0;
        }
        try {
            int value = Integer.parseInt(paramValue);
            if (value < min || value > max) {
                throw new BizParameterException(name + String.format(" value %d not in range [%d-%d]", value, min, max));
            }
            return value;
        } catch (RuntimeException e) {
            throw new BizParameterException(name + String.format(" value %s is not numeric", paramValue));
        }
    }

    public static byte getByte(HashMap<String, String> params, String name, byte min, byte max, boolean isMandatory) throws BizParameterException {
        String paramValue = Convert.emptyToNull(params.get(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new BizParameterException(name + " not specified");
            }
            return 0;
        }
        try {
            byte value = Byte.parseByte(paramValue);
            if (value < min || value > max) {
                throw new BizParameterException(name + String.format(" value %d not in range [%d-%d]", value, min, max));
            }
            return value;
        } catch (RuntimeException e) {
            throw new BizParameterException(name + String.format(" value %s is not numeric", paramValue));
        }
    }

    public static long getLong(HashMap<String, String> params, String name, long min, long max,
                               boolean isMandatory) throws BizParameterException {
        String paramValue = Convert.emptyToNull(params.get(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new BizParameterException(name + " not specified");
            }
            return 0;
        }
        try {
            long value = Long.parseLong(paramValue);
            if (value < min || value > max) {
                throw new BizParameterException(name + String.format(" value %d not in range [%d-%d]", value, min, max));
            }
            return value;
        } catch (RuntimeException e) {
            throw new BizParameterException(name + String.format(" value %s is not numeric", paramValue));
        }
    }

    public static long getUnsignedLong(HashMap<String, String> params, String name, boolean isMandatory) throws BizParameterException {
        String paramValue = Convert.emptyToNull(params.get(name));
        if (paramValue == null) {
            if (isMandatory) {
                throw new BizParameterException(name + " not specified");
            }
            return 0;
        }
        try {
            long value = Convert.parseUnsignedLong(paramValue);
            if (value == 0) { // 0 is not allowed as an id
                throw new BizParameterException("Incorrect " + name + " Value");
            }
            return value;
        } catch (RuntimeException e) {
            throw new BizParameterException("Incorrect " + name + " Value");
        }
    }

    public static long getFeeNQT(HashMap<String, String> params) throws ParameterException {
        return getLong(params, "feeNQT", 0L, Constants.MAX_BALANCE_NQT, true);
    }

    public static Attachment.TaggedDataUpload getTextData(HttpServletRequest req) throws BizParameterException, ConchException.NotValidException, ParameterException {
        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String tags = Convert.nullToEmpty(req.getParameter("tags"));
        String type = Convert.nullToEmpty(req.getParameter("type")).trim();
        String channel = Convert.nullToEmpty(req.getParameter("channel")); // Sub account
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
        String filename = Convert.nullToEmpty(req.getParameter("filename")).trim();
        String dataValue = Convert.emptyToNull(req.getParameter("data")); // uploaded data values
        byte[] data;

        if(dataValue == null) {
            throw new ParameterException(BIZ_MISSING_DATA);
        }else {
            data = Convert.toBytes(dataValue);
        }

        String detectedMimeType = Search.detectMimeType(data, filename);
        if (detectedMimeType != null) {
            isText = detectedMimeType.startsWith("text/");
            if (type.isEmpty()) {
                type = detectedMimeType.substring(0, Math.min(detectedMimeType.length(), Constants.MAX_TAGGED_DATA_TYPE_LENGTH));
            }
        }


        if (name == null) {
            throw new ParameterException(MISSING_NAME);
        }
        name = name.trim();
        if (name.length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_NAME);
        }

        if (description.length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_DESCRIPTION);
        }

        if (tags.length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_TAGS);
        }

        type = type.trim();
        if (type.length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_TYPE);
        }

        channel = channel.trim();
        if (channel.length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_CHANNEL);
        }

        if (data.length == 0 || data.length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
            throw new ParameterException(INCORRECT_DATA);
        }

        if (filename.length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_FILENAME);
        }
        return new Attachment.TaggedDataUpload(name, description, tags, type, channel, isText, filename, data);
    }

    public static Attachment.DataStorageUpload storeCache(HttpServletRequest req) throws BizParameterException, ConchException.NotValidException, ParameterException {
        //TODO validate condition
        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String type = Convert.nullToEmpty(req.getParameter("type")).trim();
        String channel = Convert.nullToEmpty(req.getParameter("channel"));
        int existence_height = Integer.parseInt(req.getParameter("existence_height"));
        int replicated_number = Integer.parseInt(req.getParameter("existence_height"));
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
        String filename = Convert.nullToEmpty(req.getParameter("filename")).trim();
        String dataValue = Convert.emptyToNull(req.getParameter("data"));
        byte[] data;
        String ssid;
        if(dataValue == null) {
            throw new ParameterException(BIZ_MISSING_DATA);
        }else {
            data = Convert.toBytes(dataValue);
        }
        ssid = IpfsService.store(data);
        String detectedMimeType = Search.detectMimeType(data, filename);
        if (detectedMimeType != null) {
            isText = detectedMimeType.startsWith("text/");
            if (type.isEmpty()) {
                type = detectedMimeType.substring(0, Math.min(detectedMimeType.length(), Constants.MAX_TAGGED_DATA_TYPE_LENGTH));
            }
        }


        if (name == null) {
            throw new ParameterException(MISSING_NAME);
        }
        name = name.trim();
        if (name.length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_NAME);
        }

        if (description.length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_DESCRIPTION);
        }

        type = type.trim();
        if (type.length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_TYPE);
        }

        channel = channel.trim();
        if (channel.length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_CHANNEL);
        }

        if (data.length == 0 || data.length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
            throw new ParameterException(INCORRECT_DATA);
        }

        if (filename.length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
            throw new ParameterException(INCORRECT_TAGGED_DATA_FILENAME);
        }
        return new Attachment.DataStorageUpload(name, description, type, ssid, channel, existence_height, replicated_number);
    }
}
