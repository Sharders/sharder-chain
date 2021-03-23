package org.conch.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @ClassName CheckNetworkUitl
 * @Description 检查是否在线
 * @Version 1.0
 **/
public class CheckNetworkUitl {

     /**
      *@Description  通过返回code判断目标节点是否正常运行
      *@Param  目标地址
      *@Return boolean
      **/
    public static boolean checkOnline(String address) {
        try {
            URL url = new URL(address);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoOutput(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setUseCaches(true);
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.connect();
            int code = httpURLConnection.getResponseCode();
            if (code == 200){
                return true;
            }
        }catch (IOException e){

        }
        return false;
    }
}
