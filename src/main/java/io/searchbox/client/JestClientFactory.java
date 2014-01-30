package io.searchbox.client;

import io.searchbox.client.config.ClientConfig;
import io.searchbox.client.config.discovery.NodeChecker;
import io.searchbox.client.http.JestHttpClient;

import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * @author Dogukan Sonmez
 */
public class JestClientFactory {

    final static Logger log = LoggerFactory.getLogger(JestClientFactory.class);
    private ClientConfig clientConfig;

    public JestClient getObject() {
        JestHttpClient client = new JestHttpClient();

        if (clientConfig != null) {
            log.debug("Creating HTTP client based on configuration");
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(clientConfig.getConnTimeout())
                    .setSocketTimeout(clientConfig.getReadTimeout())
                    .build();
            HttpClientConnectionManager connManager;
            CloseableHttpClient httpClient;
            client.setServers(clientConfig.getServerList());
            boolean isMultiThreaded = clientConfig.isMultiThreaded();
            if (isMultiThreaded) {
                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

                Integer maxTotal = clientConfig.getMaxTotalConnection();
                if (maxTotal != null) {
                    cm.setMaxTotal(maxTotal);
                }

                Integer defaultMaxPerRoute = clientConfig.getDefaultMaxTotalConnectionPerRoute();
                if (defaultMaxPerRoute != null) {
                    cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
                }

                Map<HttpRoute, Integer> maxPerRoute = clientConfig.getMaxTotalConnectionPerRoute();
                for (HttpRoute route : maxPerRoute.keySet()) {
                    cm.setMaxPerRoute(route, maxPerRoute.get(route));
                }
                
                connManager = cm;
                httpClient = HttpClients.custom()
                        .setConnectionManager(connManager)
                        .setDefaultRequestConfig(requestConfig)
                        .build();
                
                log.debug("Multi Threaded http client created");
            } else {
                connManager = new BasicHttpClientConnectionManager();
                httpClient = HttpClients.custom()
                        .setConnectionManager(connManager)
                        .setDefaultRequestConfig(requestConfig)
                        .build();
                log.debug("Default http client is created without multi threaded option");
            }

            // set custom gson instance
            Gson gson = clientConfig.getGson();
            if (gson != null) {
                client.setGson(gson);
            }
            
            client.setConnectionManager(connManager);
                        client.setDefaultRequestConfig(requestConfig);
                        client.setHttpClient(httpClient);
            //set discovery (should be set after setting the httpClient on jestClient)
            if (clientConfig.isDiscoveryEnabled()) {
                log.info("Node Discovery Enabled...");
                NodeChecker nodeChecker = new NodeChecker(clientConfig, client);
                client.setNodeChecker(nodeChecker);
                nodeChecker.startAndWait();
            } else {
                log.info("Node Discovery Disabled...");
            }
        } else {
            log.debug("There is no configuration to create http client. Going to create simple client with default values");
            client.setDefaultRequestConfig(RequestConfig.DEFAULT);
            client.setHttpClient(HttpClients.createDefault());
            LinkedHashSet<String> servers = new LinkedHashSet<String>();
            servers.add("http://localhost:9200");
            client.setServers(servers);
        }


        client.setAsyncClient(HttpAsyncClients.createDefault());
        return client;
    }

    public Class<?> getObjectType() {
        return JestClient.class;
    }

    public boolean isSingleton() {
        return false;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }
}
