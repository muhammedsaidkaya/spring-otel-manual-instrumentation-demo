package com.example.demo.controller;



import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@RestController
@RequestMapping("/api/")
public class DemoController {

    private long incomingMessageCount;
    private final MeterProvider meterProvider;
    private final Tracer tracer;
    private final RestTemplate restTemplate;

    @Autowired
    public DemoController(MeterProvider meterProvider, Tracer tracer, RestTemplate restTemplate){
        this.meterProvider = meterProvider;
        this.tracer = tracer;
        this.restTemplate = restTemplate;
        Meter meter = this.meterProvider.get("PrometheusExample");
        meter.gaugeBuilder("incoming.messages")
                .setDescription("No of incoming messages awaiting processing")
                .setUnit("message")
                .buildWithCallback(result -> result.record(incomingMessageCount, Attributes.empty()));
    }

    @GetMapping
    public String demo(){

        incomingMessageCount++;

        String response = "said";

        Span span = tracer.spanBuilder("Start my wonderful use case").startSpan();
        try {
            parentOne(span);
        } finally {
            span.end();
        }

        return response;
    }

    void parentOne(Span parent) {
        Span parentSpan = tracer.spanBuilder("parent").setParent(Context.current().with(parent)).startSpan();
        try(Scope scope = parentSpan.makeCurrent()) {
            childOne();
            childTwo();
        } finally {
            parentSpan.end();
        }
    }

    void childOne() {
        Span childSpan = tracer.spanBuilder("child-1-asda")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        try(Scope scope = childSpan.makeCurrent()) {

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>("Ugurcan", headers);
            restTemplate.exchange("http://localhost:8081/api/v1.0/print", HttpMethod.POST, entity , String.class);
        } catch (Exception e){
            System.err.println(e);
            childSpan.setStatus(StatusCode.ERROR, "Something bad happened!");
            Attributes eventAttributes2 = Attributes.of(AttributeKey.stringKey("error"), e.toString());
            childSpan.addEvent("ERROR: ", eventAttributes2);
        } finally {
            childSpan.end();
        }
    }

    void childTwo() {
        Span childSpan = tracer.spanBuilder("child-2")
                // NOTE: setParent(...) is not required;
                // `Span.current()` is automatically added as the parent
                .startSpan();
        try{
            // do stuff
        } finally {
            childSpan.end();
        }
    }
}