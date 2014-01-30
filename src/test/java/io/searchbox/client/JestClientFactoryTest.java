package io.searchbox.client;

import static junit.framework.Assert.*;
import io.searchbox.client.config.ClientConfig;
import io.searchbox.client.http.JestHttpClient;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dogukan Sonmez
 */


public class JestClientFactoryTest {

    private JestClientFactory factory;
    private ClientConfig httpClientConfig;
    
    @Before
    public void setUp() throws Exception {
        factory = new JestClientFactory();
    }

    @Test
    public void clientCreationWithTimeout() {
        ClientConfig clientConfig = new ClientConfig.Builder(
                "someUri").connTimeout(150).readTimeout(300).build();
        factory.setClientConfig(clientConfig);
        JestHttpClient jestClient = (JestHttpClient) factory.getObject();
        assertNotNull(jestClient.getDefaultRequestConfig());
        assertEquals(150, jestClient.getDefaultRequestConfig().getConnectionRequestTimeout());
        assertEquals(300, jestClient.getDefaultRequestConfig().getSocketTimeout());
    }
    @Test
    public void clientCreationWithDiscovery() {
        factory.setClientConfig(new ClientConfig.Builder("http://localhost:9200").discoveryEnabled(true).build());
        JestHttpClient jestClient = (JestHttpClient) factory.getObject();
        assertTrue(jestClient != null);
        assertNotNull(jestClient.getAsyncClient());assertTrue(jestClient.getConnectionManager() instanceof BasicHttpClientConnectionManager);
        assertEquals(jestClient.getServers().size(), 1);
        assertTrue(jestClient.getServers().contains("http://localhost:9200"));
    }

    @Test
    public void clientCreationWithNullClientConfig() {
        JestHttpClient jestClient = (JestHttpClient) factory.getObject();
        assertTrue(jestClient != null);
        assertNotNull(jestClient.getAsyncClient());
        assertEquals(jestClient.getServers().size(), 1);
        assertTrue(jestClient.getServers().contains("http://localhost:9200"));
    }

    @Test
    public void multiThreadedClientCreation() {
        HttpRoute routeOne = new HttpRoute(new HttpHost("http://test.localhost"));
        HttpRoute routeTwo = new HttpRoute(new HttpHost("http://localhost"));

        ClientConfig clientConfig = new ClientConfig.Builder("http://localhost:9200")
                .multiThreaded(true)
                .maxTotalConnection(20)
                .defaultMaxTotalConnectionPerRoute(10)
                .maxTotalConnectionPerRoute(routeOne, 5)
                .maxTotalConnectionPerRoute(routeTwo, 6)
                .build();

        factory.setClientConfig(clientConfig);
        JestHttpClient jestClient = (JestHttpClient) factory.getObject();

        assertTrue(jestClient != null);
        assertNotNull(jestClient.getAsyncClient());
        assertTrue(jestClient.getConnectionManager() instanceof PoolingHttpClientConnectionManager);
        assertEquals(10, ((PoolingHttpClientConnectionManager) jestClient.getConnectionManager()).getDefaultMaxPerRoute());
        assertEquals(20, ((PoolingHttpClientConnectionManager) jestClient.getConnectionManager()).getMaxTotal());
        assertEquals(5, ((PoolingHttpClientConnectionManager) jestClient.getConnectionManager()).getMaxPerRoute(routeOne));
        assertEquals(6, ((PoolingHttpClientConnectionManager) jestClient.getConnectionManager()).getMaxPerRoute(routeTwo));
        assertEquals(jestClient.getServers().size(), 1);
        assertTrue(jestClient.getServers().contains("http://localhost:9200"));
    }


}
