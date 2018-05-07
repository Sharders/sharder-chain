package org.sharder.util;
import org.conch.util.Logger;
import sun.net.www.protocol.https.Handler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Https {
    private static class DefaultTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    /**
     * https request
     * @param uri request url
     * @param requestMethod GET,POST
     * @param outputStr The data write to the server
     * @return
     */
    public static String httpsRequest(String uri,String requestMethod,String outputStr){
        StringBuffer buffer = null;
        SSLContext ctx = null;
        try{
            ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
            SSLSocketFactory ssf = ctx.getSocketFactory();

            URL url = new URL(null,uri,new Handler());
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            httpsConn.setSSLSocketFactory(ssf);
            httpsConn.setRequestMethod(requestMethod);
            httpsConn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            httpsConn.setDoInput(true);
            httpsConn.setDoOutput(true);
            httpsConn.connect();

            if(null != outputStr){
                OutputStream os = httpsConn.getOutputStream();
                os.write(outputStr.getBytes("utf-8"));
                os.close();
            }

            InputStream is = httpsConn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is,"utf-8");
            BufferedReader br = new BufferedReader(isr);
            buffer = new StringBuffer();
            String line = null;
            while((line = br.readLine()) != null){
                buffer.append(line);
            }
        }catch(Exception e){
            Logger.logErrorMessage("error with https request, url:" + uri + " exception:" + e.toString());
        }
        return buffer.toString();
    }

    /**
     *  http request
     * @param requestUrl request url
     * @param requestMethod GET POST
     * @param outputStr The data write to the server
     * @return
     */
    public static String httpRequest(String requestUrl,String requestMethod,String outputStr){
        StringBuffer buffer = null;
        try{
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(requestMethod);
            conn.connect();

            if(null!=outputStr){
                OutputStream os = conn.getOutputStream();
                os.write(outputStr.getBytes("utf-8"));
                os.close();
            }

            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is,"utf-8");
            BufferedReader br = new BufferedReader(isr);
            buffer = new StringBuffer();
            String line = null;
            while((line = br.readLine()) != null){
                buffer.append(line);
            }
        }catch(Exception e){
            Logger.logErrorMessage("error with http request, url:" + requestUrl + " exception:" + e.toString());
            return null;
        }
        return buffer.toString();
    }
}
