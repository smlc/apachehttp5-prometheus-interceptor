package com.vonage.api;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.ChainElement;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.message.StatusLine;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {


        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        io.prometheus.client.Counter requestCounter = io.prometheus.client.Counter.build()
                .name("request")
                .help("number of request")
                .labelNames("method")
                .register(collectorRegistry);
        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnPerRoute(5)
                .setMaxConnTotal(5)
                .setDefaultConnectionConfig(ConnectionConfig.custom().setSocketTimeout(500, TimeUnit.MILLISECONDS) // Setting socket timeout for connections
                        .setConnectTimeout(500, TimeUnit.MILLISECONDS)
                        .build())
                .build();

        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .addExecInterceptorAfter(ChainElement.RETRY.name(), "metrics", new MetricsInterceptor())
                .setConnectionManager(connectionManager)
                .build();

        client.start();
        final String requestUri = "http://httpbin.org/get";

        for (int i = 0; i < 20; i++) {
            requestCounter.labels("GET").inc();
            final SimpleHttpRequest request = SimpleRequestBuilder.get(requestUri).build();
            System.out.println("Executing request " + request);
            final Future<SimpleHttpResponse> future = client.execute(
                    request,
                    new FutureCallback<SimpleHttpResponse>() {

                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            System.out.println(request + "->" + new StatusLine(response));
                            System.out.println(response.getBody());
                        }

                        @Override
                        public void failed(final Exception ex) {
                            System.out.println(request + "->" + ex);
                        }

                        @Override
                        public void cancelled() {
                            System.out.println(request + " cancelled");
                        }

                    });
            future.get();
        }

        HTTPServer server = new HTTPServer(9400);
        System.out.println("HTTPServer listening on port http://localhost:" + server.getPort() + "/metrics");
        Thread.currentThread().join();
    }
}