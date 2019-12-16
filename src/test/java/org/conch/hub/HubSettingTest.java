package org.conch.hub;

import org.conch.base.BaseTest;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/05/10
 */
public class HubSettingTest extends BaseTest {
    static String SF_BIND_URL = "http://localhost:8080/sc/natServices/bind";
    
    static void linkSSAddress() throws ConchException.NotValidException{
        RestfulHttpClient.HttpResponse verifyResponse = null;
        try {
            String inputStr = _getFromConsole("Site Account,Account Password,Serial No,MW Address");
            String[] strArray = inputStr.split(",");
            String siteAccount = strArray[0];
            String password = strArray[1];
            String serialNo = strArray[2];
            String ssAddress = strArray[3];
            System.out.println("send check and link request to foundation[" + SF_BIND_URL + "]: " + Arrays.toString(strArray));
            verifyResponse = RestfulHttpClient.getClient(SF_BIND_URL)
                    .post()
                    .addPostParam("sharderAccount", siteAccount)
                    .addPostParam("password", password)
                    .addPostParam("nodeType", "Hub")
                    .addPostParam("serialNum", serialNo)
                    .addPostParam("tssAddress", ssAddress)
                    .request();
            com.alibaba.fastjson.JSONObject responseObj = com.alibaba.fastjson.JSONObject.parseObject(verifyResponse.getContent());
            System.out.println("response: " + responseObj.toJSONString());
            if(!responseObj.getBooleanValue(Constants.SUCCESS)) {
                throw new ConchException.NotValidException(responseObj.getString("data"));
            }
        }  catch (IOException e) {
            Logger.logErrorMessage("[ ERROR ]Failed to update linked address to foundation.", e);
            throw new ConchException.NotValidException(e.getMessage());
        }
    }


    public static void main(String[] args) {
        try {
            linkSSAddress();
        }catch (ConchException.NotValidException e) {
            System.err.println("[WARNING] linkSSAddress failed caused by: " + e.getMessage());
        }
    }
}
