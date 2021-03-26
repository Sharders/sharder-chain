package org.conch.http.biz.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.common.ConchException;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.http.*;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.h2.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.conch.http.JSONResponses.MISSING_TRANSACTION;

public class GetTransactions extends APIServlet.APIRequestHandler {

    public static final GetTransactions instance = new GetTransactions();

    private GetTransactions() {
        super(new APITag[]{APITag.BIZ}, "height", "type", "pageNo", "pageSize");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        String heightStr = Convert.emptyToNull(request.getParameter("height"));
        String type = Convert.emptyToNull(request.getParameter("type"));
        String pageNoStr = Convert.emptyToNull(request.getParameter("pageNo"));
        String pageSizeStr = Convert.emptyToNull(request.getParameter("pageSize"));
        if (heightStr == null || type == null) {
            return MISSING_TRANSACTION;
        }
        Integer height = Integer.valueOf(heightStr);
        Integer pageNo = pageNoStr != null ? Integer.valueOf(pageNoStr) : 0;
        Integer pageSize = pageSizeStr != null ? Integer.valueOf(pageSizeStr) : 0;
        if (pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        Integer firstIndex = Conch.getHeight() - height;
        Integer lastIndex = Conch.getHeight() - height;
        final int timestamp = ParameterParser.getTimestamp(request);
        JSONArray blocks = new JSONArray();
        DbIterator<? extends Block> iterator = null;
        try {
            iterator = Conch.getBlockchain().getBlocks(firstIndex, lastIndex);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getTimestamp() < timestamp) {
                    break;
                }
                if (!StringUtils.isNullOrEmpty(type)) {
                    List<String> typeList = Arrays.asList(StringUtils.arraySplit(type, ',', true));
                    List<Transaction> transactionList = block.getTransactions().stream().filter(
                            transaction -> typeList.contains(String.valueOf(transaction.getType().getType()))
                    ).collect(Collectors.toList());
                    if (transactionList.size() <= 0) {
                        continue;
                    }
                    Integer indexTypeFirst = transactionList.get(0) != null ? Integer.valueOf(transactionList.get(0).getIndex()) : 0;
                    Integer transactionIndexBegin = (pageNo - 1) * pageSize + indexTypeFirst;
                    Integer transactionIndexEnd = transactionIndexBegin + pageSize + indexTypeFirst;
                    transactionList = transactionList.stream().filter(
                            transaction -> transaction.getIndex() >= transactionIndexBegin && transaction.getIndex() < transactionIndexEnd
                    ).collect(Collectors.toList());

                    JSONObject blockJson = JSONData.block(block, true, false);
                    blockJson = JSONData.appendSpecifiedTxsBefore(transactionList, blockJson);

                    JSONArray transactionsJson = (JSONArray) blockJson.get("transactions");
                    if (transactionsJson.size() == transactionList.size()) {
                        blocks.add(blockJson);
                    }
                }
            }
        } finally {
            DbUtils.close(iterator);
        }
        JSONArray response = new JSONArray();
        ObjectMapper mapper = new ObjectMapper();
        try {
            String dtrJson = mapper.writeValueAsString(mapper.readValue(blocks.toJSONString(), new TypeReference<ArrayList<org.conch.http.biz.domain.Block>>() {
            }));
            ArrayList list = mapper.readValue(dtrJson, new TypeReference<List<Map<String, Object>>>() {
            });
            response.addAll(list);
        } catch (IOException e) {
            if (Logger.isLevel(Logger.Level.DEBUG)) {
                Logger.logErrorMessage("can't parse blocks data structure in GetTransactions api processing", e);
            } else {
                Logger.logErrorMessage("can't parse blocks data structure in GetTransactions api processing");
            }
            return JSONResponses.BIZ_JSON_IO_ERROR;
        }
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
