package com.github.deepend0.reactivestomp.test;

import com.github.deepend0.reactivestomp.messaging.messageendpoint.MessageEndpoint;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.IntStream;

@ApplicationScoped
public class SampleMessageEndpoint {
    @MessageEndpoint(inboundDestination = "/messageEndpoints/intSeries", outboundDestination = "/topics/intSeries")
    public Multi<Integer> intSeries(Integer value) {
        return Multi.createFrom().items(IntStream.range(value + 1, value + 11).boxed());
    }

    @MessageEndpoint(inboundDestination = "inboundDestination5", outboundDestination = "outboundDestination6")
    public String greetingNonAsyncValue(String name) {
        return "Bonjour " + name;
    }
}
