package com.github.deepend0.reactivestomp.test.integrationtest.messagechannel;

import com.github.deepend0.reactivestomp.websocket.ExternalMessage;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.impl.FrameParser;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.IntStream;

@QuarkusTest
@TestProfile(MessageChannelTestProfile.class)
public class MessageEndpointIntegrationTest {
    @Inject
    @Channel("serverInbound")
    private MutinyEmitter<ExternalMessage> serverInboundEmitter;

    @Inject
    private MessageChannelTestConfig messageChannelTestConfig;

    private Utils utils;

    @BeforeEach
    public void init() {
        messageChannelTestConfig.resetServerOutboundList();
        messageChannelTestConfig.resetServerOutboundHeartbeats();
        utils = new Utils(serverInboundEmitter, messageChannelTestConfig.getServerOutboundList(), messageChannelTestConfig.getServerOutboundHeartbeats());
    }

    @Test
    public void messageEndpointShouldHandleTheIncomingMessageAndReturnMultiValueShouldBePublishedToSubscribers() {
        String session1 = "session1";
        String session2 = "session2";
        String session3 = "session3";

        String subscription2 = "sub2";
        String subscription3 = "sub3";

        String sendDestination = "/messageEndpoints/intSeries";
        String subscribeDestination = "/topics/intSeries";
        int value = 10;

        long timer1 = utils.connectClient(session1);
        long timer2 = utils.connectClient(session2);
        long timer3 = utils.connectClient(session3);

        utils.subscribeClient(session2, subscription2, subscribeDestination, "1002");
        utils.subscribeClient(session3, subscription3, subscribeDestination, "1003");

        CompletableFuture<Void> cf1 = CompletableFuture.runAsync(()->utils.sendMessage(session1, sendDestination, String.valueOf(value), "1004"));
        CompletableFuture<Void> cf3 = CompletableFuture.runAsync(()->
                IntStream.range(value + 1, value + 11).boxed()
                        .forEach( i -> utils.receiveMessage(session2, subscription2, subscribeDestination, String.valueOf(i))));
        CompletableFuture<Void> cf4 = CompletableFuture.runAsync(()->
                IntStream.range(value + 1, value + 11).boxed()
                        .forEach( i -> utils.receiveMessage(session3, subscription3, subscribeDestination, String.valueOf(i))));
        CompletableFuture.allOf(cf1, cf3, cf4).join();

        utils.disconnectClient(session1, timer1, "1005");
        utils.disconnectClient(session2, timer2, "1006");
        utils.disconnectClient(session3, timer3, "1007");
    }
}
