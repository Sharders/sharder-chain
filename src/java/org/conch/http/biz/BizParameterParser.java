package org.conch.http.biz;

import org.conch.Attachment;
import org.conch.ConchException;
import org.conch.Constants;
import org.conch.util.Convert;
import org.conch.util.Search;

import javax.servlet.http.HttpServletRequest;

import static org.conch.http.JSONResponses.*;
import static org.conch.http.JSONResponses.INCORRECT_DATA;
import static org.conch.http.JSONResponses.INCORRECT_TAGGED_DATA_FILENAME;

public class BizParameterParser  {

    public static Attachment.TaggedDataUpload getTextData(HttpServletRequest req) throws BizParameterException, ConchException.NotValidException {
        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String tags = Convert.nullToEmpty(req.getParameter("tags"));
        String type = Convert.nullToEmpty(req.getParameter("type")).trim();
        String channel = Convert.nullToEmpty(req.getParameter("channel"));
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
        String filename = Convert.nullToEmpty(req.getParameter("filename")).trim();
        String dataValue = Convert.emptyToNull(req.getParameter("data"));
        byte[] data;

        if(dataValue == null) {
            throw new BizParameterException(BIZ_MISSING_DATA);
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
            throw new BizParameterException(MISSING_NAME);
        }
        name = name.trim();
        if (name.length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
            throw new BizParameterException(INCORRECT_TAGGED_DATA_NAME);
        }

        if (description.length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
            throw new BizParameterException(INCORRECT_TAGGED_DATA_DESCRIPTION);
        }

        if (tags.length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
            throw new BizParameterException(INCORRECT_TAGGED_DATA_TAGS);
        }

        type = type.trim();
        if (type.length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
            throw new BizParameterException(INCORRECT_TAGGED_DATA_TYPE);
        }

        channel = channel.trim();
        if (channel.length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
            throw new BizParameterException(INCORRECT_TAGGED_DATA_CHANNEL);
        }

        if (data.length == 0 || data.length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
            throw new BizParameterException(INCORRECT_DATA);
        }

        if (filename.length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
            throw new BizParameterException(INCORRECT_TAGGED_DATA_FILENAME);
        }
        return new Attachment.TaggedDataUpload(name, description, tags, type, channel, isText, filename, data);
    }
}
