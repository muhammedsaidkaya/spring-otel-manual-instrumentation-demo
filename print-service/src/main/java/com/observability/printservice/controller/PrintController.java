package com.observability.printservice.controller;

import com.observability.printservice.utils.Utils;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
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
    private final Tracer tracer;

    @Autowired
    public PrintController(Tracer tracer){
        this.tracer = tracer;
    }

    @PostMapping
    public String print(@RequestBody String message){
        Span parentSpan = tracer.spanBuilder("print-service-parent").setSpanKind(SpanKind.SERVER).startSpan();
        try {
            LOG.info("Getting message: asd");
        } finally {
            parentSpan.end();
        }
        return message;
    }
}
