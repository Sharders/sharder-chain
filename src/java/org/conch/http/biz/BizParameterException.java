package org.conch.http.biz;

import org.conch.http.ParameterException;
import org.json.simple.JSONStreamAware;

public class BizParameterException extends ParameterException {


    BizParameterException(JSONStreamAware errorResponse) {
        super(errorResponse);
    }
}