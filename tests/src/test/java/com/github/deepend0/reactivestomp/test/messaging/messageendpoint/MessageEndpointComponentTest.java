package com.github.deepend0.reactivestomp.test.messaging.messageendpoint;

import com.github.deepend0.reactivestomp.websocket.ExternalMessage;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.impl.FrameParser;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@QuarkusTest
public class MessageEndpointComponentTest {

    private final Vertx vertx = Vertx.vertx();

    @Inject
    @Channel("serverInbound")
    private MutinyEmitter<ExternalMessage> serverInboundEmitter;
    @Inject
    @Channel("serverOutbound")
    private Multi<ExternalMessage> serverOutboundReceiver;

    private Deque<ExternalMessage> serverOutboundList = new ConcurrentLinkedDeque<>();
    private Deque<ExternalMessage> serverOutboundHeartbeats = new ConcurrentLinkedDeque<>();

    @BeforeEach
    public void init() {
        serverOutboundReceiver.subscribe().with(externalMessage -> {
            if (Arrays.equals(externalMessage.message(), Buffer.buffer(FrameParser.EOL).getBytes())
                    || Arrays.equals(externalMessage.message(), new byte[]{'\0'})) {
                serverOutboundHeartbeats.add(externalMessage);
            } else {
                serverOutboundList.add(externalMessage);
            }
        });
    }
}
