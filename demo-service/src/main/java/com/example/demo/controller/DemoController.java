package com.example.demo.controller;



import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/api/")
public class DemoController {

    private long incomingMessageCount;
    private final MeterProvider meterProvider;
    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;
    TextMapSetter<HttpURLConnection> setter =
            new TextMapSetter<HttpURLConnection>() {
                @Override
                public void set(HttpURLConnection carrier, String key, String value) {
                    // Insert the context as Header
                    carrier.setRequestProperty(key, value);
                }
            };

    @Autowired
    public DemoController(MeterProvider meterProvider, OpenTelemetry openTelemetry, Tracer tracer){
        this.meterProvider = meterProvider;
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
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

            URL url = new URL("http://localhost:8081/api/v1.0/print");
            //ADD EVENT
            Attributes eventAttributes = Attributes.of(
                    SemanticAttributes.HTTP_METHOD, "POST",
                    SemanticAttributes.HTTP_URL, url.toString());
            childSpan.addEvent("Request To Print Service", eventAttributes);

            //HTTP CONNECTION OPEN
            HttpURLConnection transportLayer = (HttpURLConnection) url.openConnection();
            transportLayer.setDoInput(true);
            transportLayer.setDoOutput(true);

            //REQUEST_INJECT CONTEXT
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), transportLayer, setter);

            //SEND-REQUEST
            String jsonInputString = "Ugurcan";
            byte[] input = jsonInputString.getBytes("utf-8");
            transportLayer.getOutputStream().write(input, 0 , input.length);

            //GET-RESPONSE
            String responseString = null;
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(transportLayer.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseString = response.toString();
                System.out.println(responseString);
            }
            Attributes eventAttributes2 = Attributes.of(AttributeKey.stringKey("response"), responseString);
            childSpan.addEvent("Response From Print Service", eventAttributes2);
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