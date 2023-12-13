package com.contentgrid.surveyor.application.exporter.postgres.boot;

import io.prometheus.metrics.exporter.common.PrometheusHttpExchange;
import io.prometheus.metrics.exporter.common.PrometheusHttpRequest;
import io.prometheus.metrics.exporter.common.PrometheusHttpResponse;
import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.Executor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Controller
@RequiredArgsConstructor
public class MetricsController {

    private final PrometheusScrapeHandler scrapeHandler;
    private final Executor executor;

    @GetMapping({
            "/metrics",
            "/actuator/prometheus"
    })
    public Mono<Void> metrics(ServerWebExchange exchange) throws IOException {
        var httpExchange = new HttpExchangeAdapter(exchange.getRequest(), exchange.getResponse());

        executor.execute(() -> {
            try {
                scrapeHandler.handleRequest(httpExchange);
            } catch (IOException e) {
                httpExchange.handleException(e);
            } catch (RuntimeException e) {
                httpExchange.handleException(e);
            }
        });
        return httpExchange.awaitComplete();
    }

    @RequiredArgsConstructor
    private static class HttpRequestAdapter implements PrometheusHttpRequest {
        private final ServerHttpRequest request;


        @Override
        public String getQueryString() {
            return request.getURI().getQuery();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return new Vector<>(request.getHeaders().getOrEmpty(name)).elements();
        }

        @Override
        public String getMethod() {
            return request.getMethod().name();
        }

        @Override
        public String getRequestPath() {
            return request.getURI().getPath();
        }
    }

    @RequiredArgsConstructor
    private static class HttpResponseAdapter implements PrometheusHttpResponse {
        private final ServerHttpResponse response;

        @Override
        public void setHeader(String name, String value) {
            response.getHeaders().set(name, value);
        }

        @Override
        public OutputStream sendHeadersAndGetBody(int statusCode, int contentLength) throws IOException {
            response.setRawStatusCode(statusCode);

            var dataBuffer = response.bufferFactory().allocateBuffer(contentLength);
            return new OutputStream() {
                @Override
                public void write(byte[] b) throws IOException {
                    dataBuffer.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    dataBuffer.write(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    response.writeWith(Mono.just(dataBuffer)).block();
                }

                @Override
                public void write(int b) throws IOException {
                    dataBuffer.write((byte)b);
                }
            };
        }
    }

    @RequiredArgsConstructor
    private static class HttpExchangeAdapter implements PrometheusHttpExchange {
        @Getter
        private final PrometheusHttpRequest request;
        @Getter
        private final HttpResponseAdapter response;

        private final Sinks.One<Void> completeSink = Sinks.one();

        public HttpExchangeAdapter(ServerHttpRequest request, ServerHttpResponse response) {
            this(new HttpRequestAdapter(request), new HttpResponseAdapter(response));
        }

        @Override
        public void handleException(IOException e) {
            completeSink.tryEmitError(e);
        }

        @Override
        public void handleException(RuntimeException e) {
            completeSink.tryEmitError(e);
        }

        @Override
        public void close() {
            response.response.setComplete().block();
            completeSink.tryEmitValue(null);
        }

        public Mono<Void> awaitComplete() {
            return completeSink.asMono();
        }
    }

}
