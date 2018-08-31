package org.conch.http;

import org.conch.Block;
import org.conch.Conch;
import org.conch.ConchException;
import org.conch.util.Convert;
import org.conch.vm.db.AccountState;
import org.conch.vm.db.Repository;
import org.conch.vm.db.RepositoryImpl;
import org.conch.vm.util.ByteUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class ContractInfo extends APIServlet.APIRequestHandler {
    static final ContractInfo instance = new ContractInfo();

    private ContractInfo() {
        super(new APITag[]{APITag.CONTRACT}, "address", "blockHeight");
    }


    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        String address = Convert.emptyToNull(request.getParameter("address"));
        String height = request.getParameter("blockHeight");
        Block block = null;
        if (height == null)
            block = Conch.getBlockchain().getLastBlock();
        else
            block = Conch.getBlockchain().getBlockAtHeight(Integer.parseInt(height));
        Repository repository = new RepositoryImpl(block.getStateRoot());
        AccountState accountState = repository.getAccountState(ByteUtil.hexStringToBytes(address));
        JSONObject response = new JSONObject();
        response.put("accountState", accountState.toString());
        return response;
    }
}
