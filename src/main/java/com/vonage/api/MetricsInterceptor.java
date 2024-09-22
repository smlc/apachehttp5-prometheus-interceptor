package com.vonage.api;

import io.prometheus.client.Histogram;
import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.client5.http.async.AsyncExecChain;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;

import java.io.IOException;

public class MetricsInterceptor implements AsyncExecChainHandler {
    private Histogram LATENCY;

    public MetricsInterceptor() {
        LATENCY = Histogram.build()
                .namespace("metrics_http")
                .name("latency")
                .labelNames("method", "host", "path", "status")
                .help("Duration for request")
                .register();
    }

    @Override
    public void execute(HttpRequest request, AsyncEntityProducer entityProducer, AsyncExecChain.Scope scope, AsyncExecChain chain, AsyncExecCallback asyncExecCallback) throws HttpException, IOException {
        final String method = request.getMethod();
        final String host = scope.route.getTargetHost().getHostName();
        final String path = request.getPath();
        final double startTime = System.nanoTime();
        chain.proceed(request, entityProducer, scope, new AsyncExecCallback(){

            @Override
            public AsyncDataConsumer handleResponse(HttpResponse response, EntityDetails entityDetails) throws HttpException, IOException {
                double duration = (System.nanoTime() - startTime) / 1e9; //to seconds
                LATENCY.labels(method, host, path, String.valueOf(response.getCode()))
                        .observe(duration);
                return asyncExecCallback.handleResponse(response, entityDetails);
            }

            @Override
            public void handleInformationResponse(HttpResponse response) throws HttpException, IOException {
                asyncExecCallback.handleInformationResponse(response);
            }

            @Override
            public void completed() {
                asyncExecCallback.completed();
            }

            @Override
            public void failed(Exception cause) {
                double duration = (System.nanoTime() - startTime) / 1e9;
                LATENCY.labels(method, host, path, "error").observe(duration);
                asyncExecCallback.failed(cause);
            }
        });
    }
}
