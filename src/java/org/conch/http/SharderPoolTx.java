package org.conch.http;

import org.conch.ConchException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/24
 */
public abstract class SharderPoolTx {

    public final class CreateTx extends CreateTransaction {
        protected CreateTx(APITag[] apiTags, String... parameters) {
            super(apiTags, parameters);
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            return null;
        }
    }

    public final class DestoryTx extends CreateTransaction {
        protected DestoryTx(APITag[] apiTags, String... parameters) {
            super(apiTags, parameters);
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
            return null;
        }
    }
}
