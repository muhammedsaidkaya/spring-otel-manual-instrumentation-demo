package com.observability.printservice.controller;

import com.observability.printservice.utils.Utils;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1.0/print")
@CrossOrigin
public class PrintController {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private long incomingMessageCount;

    private final MeterProvider meterProvider;
    private final OpenTelemetry openTelemetry;
    private Tracer tracer;

    @Autowired
    public PrintController(MeterProvider meterProvider, OpenTelemetry openTelemetry, Tracer tracer){
        this.meterProvider = meterProvider;
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
        Meter meter = this.meterProvider.get("PrometheusExample");
        meter.gaugeBuilder("incoming.messages")
            .setDescription("No of incoming messages awaiting processing")
            .setUnit("message")
            .buildWithCallback(result -> result.record(incomingMessageCount, Attributes.empty()));
    }

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

    @PostMapping
    public String print(@RequestBody String message, HttpServletRequest httpServletRequest){

        incomingMessageCount++;

        Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), httpServletRequest, getter);
        try (Scope scope = extractedContext.makeCurrent()) {
            Span parentSpan = tracer.spanBuilder("print-service-parent").setSpanKind(SpanKind.SERVER).startSpan();
            try {
                LOG.info("Getting message: asd");
            } finally {
                parentSpan.end();
            }
        }
        return message;
    }
}
