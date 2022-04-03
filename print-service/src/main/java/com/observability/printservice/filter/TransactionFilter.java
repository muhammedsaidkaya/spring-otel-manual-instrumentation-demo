package com.observability.printservice.filter;

import com.observability.printservice.utils.Utils;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class TransactionFilter implements Filter {

    private final OpenTelemetry openTelemetry;
    private long incomingMessageCount;
    private final MeterProvider meterProvider;

    TextMapGetter<HttpServletRequest> getter = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest httpServletRequest) {
            return Utils.iterable(httpServletRequest.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest httpServletRequest, String s) {
            assert httpServletRequest != null;
            return httpServletRequest.getHeader(s);
        }
    };

    @Autowired
    public TransactionFilter(OpenTelemetry openTelemetry, MeterProvider meterProvider) {
        this.openTelemetry = openTelemetry;
        this.meterProvider = meterProvider;
        Meter meter = this.meterProvider.get("PrometheusExample");
        meter.gaugeBuilder("incoming.messages")
                .setDescription("No of incoming messages awaiting processing")
                .setUnit("message")
                .buildWithCallback(result -> result.record(incomingMessageCount, Attributes.empty()));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        incomingMessageCount++;
        HttpServletRequest req = (HttpServletRequest) request;
        Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), req, getter);
        try (Scope scope = extractedContext.makeCurrent()) {
            chain.doFilter(request, response);
        }
    }
}