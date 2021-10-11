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

package org.conch.http;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.conch.http.biz.BizServlet;
import org.conch.peer.Peers;
import org.conch.security.Guard;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;
import org.conch.util.UPnP;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.conch.http.JSONResponses.*;

public final class API {

    public static final int TESTNET_API_PORT = 8215;
    public static final int TESTNET_API_SSLPORT = 8218;
    private static final String[] DISABLED_HTTP_METHODS = {"TRACE", "OPTIONS", "HEAD"};

    public static final int openAPIPort;
    public static final int openAPISSLPort;

    public static final List<String> disabledAPIs;
    public static final List<APITag> disabledAPITags;

    private static final Set<String> allowedBotHosts;
    private static final List<NetworkAddress> allowedBotNets;
    private static final Map<String, PasswordCount> incorrectPasswords = new HashMap<>();
    public static final String adminPassword = Conch.getStringProperty("sharder.adminPassword", "", true);
    static final boolean disableAdminPassword;
    static final int maxRecords = Conch.getIntProperty("sharder.maxAPIRecords");
    static final boolean enableAPIUPnP = Conch.getBooleanProperty("sharder.enableAPIUPnP");
    public static final int apiServerIdleTimeout = Conch.getIntProperty("sharder.apiServerIdleTimeout");
    public static final boolean apiServerCORS = Conch.getBooleanProperty("sharder.apiServerCORS");

    private static final Server apiServer;
    private static URI welcomePageUri;
    private static URI serverRootUri;

    static {
        List<String> disabled = new ArrayList<>(Conch.getStringListProperty("sharder.disabledAPIs"));
        Collections.sort(disabled);
        disabledAPIs = Collections.unmodifiableList(disabled);
        disabled = Conch.getStringListProperty("sharder.disabledAPITags");

        if (disabled.isEmpty() && !Conch.getBooleanProperty("sharder.enableBizAPIs")){
            disabled = new ArrayList<String>();
            disabled.add(APITag.BIZ.getDisplayName());
        }
        Collections.sort(disabled);
        List<APITag> apiTags = new ArrayList<>(disabled.size());
        disabled.forEach(tagName -> apiTags.add(APITag.fromDisplayName(tagName)));
        disabledAPITags = Collections.unmodifiableList(apiTags);
        List<String> allowedBotHostsList = Conch.getStringListProperty("sharder.allowedBotHosts");
        if (! allowedBotHostsList.contains("*")) {
            Set<String> hosts = new HashSet<>();
            List<NetworkAddress> nets = new ArrayList<>();
            for (String host : allowedBotHostsList) {
                if (host.contains("/")) {
                    try {
                        nets.add(new NetworkAddress(host));
                    } catch (UnknownHostException e) {
                        Logger.logErrorMessage("Unknown network " + host, e);
                        throw new RuntimeException(e.toString(), e);
                    }
                } else {
                    hosts.add(host);
                }
            }
            allowedBotHosts = Collections.unmodifiableSet(hosts);
            allowedBotNets = Collections.unmodifiableList(nets);
        } else {
            allowedBotHosts = null;
            allowedBotNets = null;
        }

        boolean enableAPIServer = Conch.getBooleanProperty("sharder.enableAPIServer");
        if (enableAPIServer) {
            final int port = Conch.getApiPort();
            final int sslPort = Conch.getApiSSLPort();
            final String host = Conch.getStringProperty("sharder.apiServerHost");
            disableAdminPassword = Conch.getBooleanProperty("sharder.disableAdminPassword") || ("127.0.0.1".equals(host) && adminPassword.isEmpty());

            apiServer = new Server();
            ServerConnector connector;
            boolean enableSSL = Conch.getBooleanProperty("sharder.apiSSL");
            //
            // Create the HTTP connector
            //
            if (!enableSSL || port != sslPort) {
                HttpConfiguration configuration = new HttpConfiguration();
                configuration.setSendDateHeader(false);
                configuration.setSendServerVersion(false);

                connector = new ServerConnector(apiServer, new HttpConnectionFactory(configuration));
                connector.setPort(port);
                connector.setHost(host);
                connector.setIdleTimeout(apiServerIdleTimeout);
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                Logger.logMessage("API server using HTTP port " + port);
            }
            //
            // Create the HTTPS connector
            //
            final SslContextFactory sslContextFactory;
            if (enableSSL) {
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSendDateHeader(false);
                https_config.setSendServerVersion(false);
                https_config.setSecureScheme("https");
                https_config.setSecurePort(sslPort);
                https_config.addCustomizer(new SecureRequestCustomizer());
                sslContextFactory = new SslContextFactory();
                String keyStorePath = Paths.get(Conch.getUserHomeDir()).resolve(Paths.get(Conch.getStringProperty("sharder.keyStorePath"))).toString();
                Logger.logInfoMessage("Using keystore: " + keyStorePath);
                sslContextFactory.setKeyStorePath(keyStorePath);
                sslContextFactory.setKeyStorePassword(Conch.getStringProperty("sharder.keyStorePassword", null, true));
                sslContextFactory.addExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                sslContextFactory.addExcludeProtocols("SSLv3");
                sslContextFactory.setKeyStoreType(Conch.getStringProperty("sharder.keyStoreType"));
                List<String> ciphers = Conch.getStringListProperty("sharder.apiSSLCiphers");
                if (!ciphers.isEmpty()) {
                    sslContextFactory.setIncludeCipherSuites(ciphers.toArray(new String[ciphers.size()]));
                }
                connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(https_config));
                connector.setPort(sslPort);
                connector.setHost(host);
                connector.setIdleTimeout(apiServerIdleTimeout);
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                Logger.logMessage("API server using HTTPS port " + sslPort);
            } else {
                sslContextFactory = null;
            }
            String localhost = "0.0.0.0".equals(host) || "127.0.0.1".equals(host) ? "localhost" : host;
            try {
                welcomePageUri = new URI(enableSSL ? "https" : "http", null, localhost, enableSSL ? sslPort : port, "/index.html", null, null);
                serverRootUri = new URI(enableSSL ? "https" : "http", null, localhost, enableSSL ? sslPort : port, "", null, null);
            } catch (URISyntaxException e) {
                Logger.logInfoMessage("Cannot resolve browser URI", e);
            }
            openAPIPort = !Constants.isLightClient && "0.0.0.0".equals(host) && allowedBotHosts == null && (!enableSSL || port != sslPort) ? port : 0;
            openAPISSLPort = !Constants.isLightClient && "0.0.0.0".equals(host) && allowedBotHosts == null && enableSSL ? sslPort : 0;

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = Conch.getStringProperty("sharder.apiResourceBase");
            if (apiResourceBase != null) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                defaultServletHolder.setInitParameter("etags", "true");
                apiHandler.addServlet(defaultServletHolder, "/*");
                apiHandler.addFilter(CosRouteFilter.class, "/*", null);
                apiHandler.setWelcomeFiles(new String[]{Conch.getStringProperty("sharder.apiWelcomeFile")});
            }

            String javadocResourceBase = Conch.getStringProperty("sharder.javadocResourceBase");
            if (javadocResourceBase != null) {
                ContextHandler contextHandler = new ContextHandler("/doc");
                ResourceHandler docFileHandler = new ResourceHandler();
                docFileHandler.setDirectoriesListed(false);
                docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                docFileHandler.setResourceBase(javadocResourceBase);
                contextHandler.setHandler(docFileHandler);
                apiHandlers.addHandler(contextHandler);
            }

            ServletHolder servletHolder = apiHandler.addServlet(APIServlet.class, "/sharder");
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(
                    null, Math.max(Conch.getIntProperty("sharder.maxUploadFileSize"), Constants.MAX_TAGGED_DATA_DATA_LENGTH), -1L, 0));

            servletHolder = apiHandler.addServlet(APIProxyServlet.class, "/sharder-proxy");
            servletHolder.setInitParameters(Collections.singletonMap("idleTimeout",
                    "" + Math.max(apiServerIdleTimeout - APIProxyServlet.PROXY_IDLE_TIMEOUT_DELTA, 0)));
            servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(
                    null, Math.max(Conch.getIntProperty("sharder.maxUploadFileSize"), Constants.MAX_TAGGED_DATA_DATA_LENGTH), -1L, 0));
            apiHandler.addServlet(ShapeShiftProxyServlet.class, ShapeShiftProxyServlet.SHAPESHIFT_TARGET + "/*");

            GzipHandler gzipHandler = new GzipHandler();
            if (!Conch.getBooleanProperty("sharder.enableAPIServerGZIPFilter")) {
                gzipHandler.setExcludedPaths("/sharder", "/sharder-proxy");
            }
            gzipHandler.setIncludedMethods("GET", "POST");
            gzipHandler.setMinGzipSize(Peers.MIN_COMPRESS_SIZE);
            apiHandler.setGzipHandler(gzipHandler);

            apiHandler.addServlet(APITestServlet.class, "/debug");
            apiHandler.addServlet(APITestServlet.class, "/debug-proxy");
            apiHandler.addServlet(DbShellServlet.class, "/dbshell");

            if (apiServerCORS) {
                FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            if (Conch.getBooleanProperty("sharder.apiFrameOptionsSameOrigin") && !Constants.isLightClient) {
                FilterHolder filterHolder = apiHandler.addFilter(XFrameOptionsFilter.class, "/*", null);
                filterHolder.setAsyncSupported(true);
            }
            disableHttpMethods(apiHandler);

            // Added request params filter for BizAPIs
            apiHandler.addFilter(BizRequestParamFilter.class, "/sharder", null);
            // Defined business APIs Servlet
            apiHandler.addServlet(BizServlet.class, "/rpc");
            apiHandlers.addHandler(apiHandler);
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.setStopAtShutdown(true);
            // Set maxFormContentSize to -1 to unlimited Form size to support large data upload from http
            apiServer.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", -1);

            ThreadPool.runBeforeStart(() -> {
                try {
                    if (enableAPIUPnP) {
                        Connector[] apiConnectors = apiServer.getConnectors();
                        for (Connector apiConnector : apiConnectors) {
                            if (apiConnector instanceof ServerConnector)
                                UPnP.addPort(((ServerConnector)apiConnector).getPort());
                        }
                    }
                    APIServlet.initClass();
                    APIProxyServlet.initClass();
                    APITestServlet.initClass();
                    apiServer.start();
                    if (sslContextFactory != null) {
                        Logger.logDebugMessage("API SSL Protocols: " + Arrays.toString(sslContextFactory.getSelectedProtocols()));
                        Logger.logDebugMessage("API SSL Ciphers: " + Arrays.toString(sslContextFactory.getSelectedCipherSuites()));
                    }
                    Logger.logMessage("Started API server at " + host + ":" + port + (enableSSL && port != sslPort ? ", " + host + ":" + sslPort : ""));
                } catch (Exception e) {
                    Logger.logErrorMessage("Failed to start API server", e);
                    throw new RuntimeException(e.toString(), e);
                }

            }, true);

        } else {
            apiServer = null;
            disableAdminPassword = false;
            openAPIPort = 0;
            openAPISSLPort = 0;
            Logger.logMessage("API server not enabled");
        }

    }

    static {
        try {
            if (Airdrop.DEFAULT_PATH_NAME != null) {
                ThreadPool.scheduleThread("AutoAirdrop", Airdrop.autoAirdropThread, 1, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            Logger.logErrorMessage("Failed to run custom logic", e);
        }
    }

    public static void init() {}

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
                if (enableAPIUPnP) {
                    Connector[] apiConnectors = apiServer.getConnectors();
                    for (Connector apiConnector : apiConnectors) {
                        if (apiConnector instanceof ServerConnector)
                            UPnP.deletePort(((ServerConnector)apiConnector).getPort());
                    }
                }
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop API server", e);
            }
        }
    }

    public static void verifyPassword(HttpServletRequest req) throws ParameterException {
        if (API.disableAdminPassword) {
            return;
        }
        // when init hub, do not need admin password
        if (Boolean.TRUE.toString().equalsIgnoreCase(req.getParameter("isInit"))) {
            return;
        }
        if (API.adminPassword.isEmpty()) {
            throw new ParameterException(NO_PASSWORD_IN_CONFIG);
        }
        checkOrLockPassword(req);
    }

    public static boolean checkPassword(HttpServletRequest req) {
        if (API.disableAdminPassword) {
            return true;
        }
        if (API.adminPassword.isEmpty()) {
            return false;
        }
        if (Convert.emptyToNull(req.getParameter("adminPassword")) == null) {
            return false;
        }
        try {
            checkOrLockPassword(req);
            return true;
        } catch (ParameterException e) {
            return false;
        }
    }


    private static class PasswordCount {
        private int count;
        private int time;
    }

    private static void checkOrLockPassword(HttpServletRequest req) throws ParameterException {
        int now = Conch.getEpochTime();
        String remoteHost = req.getRemoteHost();
        synchronized(incorrectPasswords) {
            PasswordCount passwordCount = incorrectPasswords.get(remoteHost);
            if (passwordCount != null && passwordCount.count >= 3 && now - passwordCount.time < 60*60) {
                if (Logger.printNow(Logger.API_incorrectAdmPwd)) {
                    Logger.logWarningMessage("Too many incorrect admin password attempts from " + remoteHost);
                }
                Guard.viciousAccess(remoteHost);
                throw new ParameterException(LOCKED_ADMIN_PASSWORD);
            }
            if (!API.adminPassword.equals(req.getParameter("adminPassword"))) {
                if (passwordCount == null) {
                    passwordCount = new PasswordCount();
                    incorrectPasswords.put(remoteHost, passwordCount);
                }
                passwordCount.count++;
                passwordCount.time = now;

                Guard.viciousAccess(remoteHost);
                if (Logger.printNow(Logger.API_incorrectAdmPwd)) {
                    Logger.logWarningMessage("Incorrect adminPassword from " + remoteHost);
                }
                throw new ParameterException(INCORRECT_ADMIN_PASSWORD);
            }
            if (passwordCount != null) {
                incorrectPasswords.remove(remoteHost);
            }
        }
    }

    static boolean isAllowed(String remoteHost) {
        if (API.allowedBotHosts == null || API.allowedBotHosts.contains(remoteHost)) {
            return true;
        }
        try {
            BigInteger hostAddressToCheck = new BigInteger(InetAddress.getByName(remoteHost).getAddress());
            for (NetworkAddress network : API.allowedBotNets) {
                if (network.contains(hostAddressToCheck)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // can't resolve, disallow
            Logger.logMessage("Unknown remote host " + remoteHost);
        }
        return false;

    }

    private static void disableHttpMethods(ServletContextHandler servletContext) {
        SecurityHandler securityHandler = servletContext.getSecurityHandler();
        if (securityHandler == null) {
            securityHandler = new ConstraintSecurityHandler();
            servletContext.setSecurityHandler(securityHandler);
        }
        disableHttpMethods(securityHandler);
    }

    private static void disableHttpMethods(SecurityHandler securityHandler) {
        if (securityHandler instanceof ConstraintSecurityHandler) {
            ConstraintSecurityHandler constraintSecurityHandler = (ConstraintSecurityHandler) securityHandler;
            for (String method : DISABLED_HTTP_METHODS) {
                disableHttpMethod(constraintSecurityHandler, method);
            }
            ConstraintMapping enableEverythingButTraceMapping = new ConstraintMapping();
            Constraint enableEverythingButTraceConstraint = new Constraint();
            enableEverythingButTraceConstraint.setName("Enable everything but TRACE");
            enableEverythingButTraceMapping.setConstraint(enableEverythingButTraceConstraint);
            enableEverythingButTraceMapping.setMethodOmissions(DISABLED_HTTP_METHODS);
            enableEverythingButTraceMapping.setPathSpec("/");
            constraintSecurityHandler.addConstraintMapping(enableEverythingButTraceMapping);
        }
    }

    private static void disableHttpMethod(ConstraintSecurityHandler securityHandler, String httpMethod) {
        ConstraintMapping mapping = new ConstraintMapping();
        Constraint constraint = new Constraint();
        constraint.setName("Disable " + httpMethod);
        constraint.setAuthenticate(true);
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/");
        mapping.setMethod(httpMethod);
        securityHandler.addConstraintMapping(mapping);
    }

    private static class NetworkAddress {

        private BigInteger netAddress;
        private BigInteger netMask;

        private NetworkAddress(String address) throws UnknownHostException {
            String[] addressParts = address.split("/");
            if (addressParts.length == 2) {
                InetAddress targetHostAddress = InetAddress.getByName(addressParts[0]);
                byte[] srcBytes = targetHostAddress.getAddress();
                netAddress = new BigInteger(1, srcBytes);
                int maskBitLength = Integer.valueOf(addressParts[1]);
                int addressBitLength = (targetHostAddress instanceof Inet4Address) ? 32 : 128;
                netMask = BigInteger.ZERO
                        .setBit(addressBitLength)
                        .subtract(BigInteger.ONE)
                        .subtract(BigInteger.ZERO.setBit(addressBitLength - maskBitLength).subtract(BigInteger.ONE));
            } else {
                throw new IllegalArgumentException("Invalid address: " + address);
            }
        }

        private boolean contains(BigInteger hostAddressToCheck) {
            return hostAddressToCheck.and(netMask).equals(netAddress);
        }

    }

    public static final class XFrameOptionsFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            String options = "";
            if(AuthorizationLogin.hashMap.get(request.getParameter("token")) == null){
                options = "SAMEORIGIN";
            }
            ((HttpServletResponse) response).setHeader("X-FRAME-OPTIONS", options);
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }

    }

    public static final class BizRequestParamFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            Map<String,String[]> m = new HashMap<String,String[]>(request.getParameterMap());
            request = new BizParameterRequestWrapper((HttpServletRequest)request, m);

            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }

    }

    public static final class CosRouteFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest request= (HttpServletRequest) req;

            String urlStr = request.getRequestURI();
           
            if(!urlStr.startsWith("/sharder")){
                HttpServletResponse response = (HttpServletResponse) resp;

                if(isRoute(urlStr)) {
                    Logger.logDebugMessage("urlStr=" + urlStr + ",PathInfo=" + request.getPathInfo() + ",Method=" + request.getMethod() + ",request=" + request.toString());
                    response.sendRedirect("/index.html");
                }
            }
            chain.doFilter(req, resp);
        }

        @Override
        public void destroy() {}

    }

    public static URI getWelcomePageUri() {
        return welcomePageUri;
    }

    public static URI getServerRootUri() {
        return serverRootUri;
    }
    
    static Set<String> cosRoute = Sets.newHashSet("/login", "/register", "/enter", "/account", "/network", "/mining");
    public static boolean isRoute(String targetUrl){
        for(String route : cosRoute){
            if(StringUtils.isNotEmpty(targetUrl) && targetUrl.contains(route)){
                return true;
            }
        }
        return false;
    }
    
    private API() {} // never

}
