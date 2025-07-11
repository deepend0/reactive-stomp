package com.github.deepend0.reactivestomp.test.integrationtest.websocket;

import com.github.deepend0.reactivestomp.test.HeartbeatManager;
import com.github.deepend0.reactivestomp.test.StompWebSocketClient;
import com.github.deepend0.reactivestomp.test.stompprocessor.FrameTestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.impl.FrameParser;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
@TestProfile(WebSocketTestProfile.class)
public class WebSocketIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketIntegrationTest.class);
    public static final Duration AWAIT_AT_MOST = Duration.ofMillis(3000);
    public static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(100);

    @Inject
    private StompWebSocketClient stompWebSocketClient;

    @Inject
    private HeartbeatManager heartbeatManager;

    private final Vertx vertx = Vertx.vertx();

    @Test
    public void shouldSendMessageToMultipleBrokerClients() {
        Deque<byte[]> receivedMessages1 = new LinkedList<>();
        Deque<byte[]> receivedHeartbeats1 = new LinkedList<>();
        WebSocketClientConnection clientConnection1 = createWebSocketConnection("client1", receivedMessages1, receivedHeartbeats1);
        Deque<byte[]> receivedMessages2 = new LinkedList<>();
        Deque<byte[]> receivedHeartbeats2 = new LinkedList<>();
        WebSocketClientConnection clientConnection2 = createWebSocketConnection("client2", receivedMessages2, receivedHeartbeats2);
        Deque<byte[]> receivedMessages3 = new LinkedList<>();
        Deque<byte[]> receivedHeartbeats3 = new LinkedList<>();
        WebSocketClientConnection clientConnection3 = createWebSocketConnection("client3", receivedMessages3, receivedHeartbeats3);

        connectClient(clientConnection1, receivedMessages1, receivedHeartbeats1);
        connectClient(clientConnection2, receivedMessages2, receivedHeartbeats2);
        connectClient(clientConnection3, receivedMessages3, receivedHeartbeats3);

        String subscription1 = "sub1";
        String subscription2 = "sub2";
        String subscription3 = "sub3";

        String destination = "/topic/chat";
        String message = "Hello World!";

        subscribeClient(clientConnection1, receivedMessages1, subscription1, destination, "1001");
        subscribeClient(clientConnection2, receivedMessages2, subscription2, destination, "1002");
        subscribeClient(clientConnection3, receivedMessages3, subscription3, destination, "1003");

        CompletableFuture<Void> cf1 = CompletableFuture.runAsync(()-> sendMessage(clientConnection1, receivedMessages1, destination, message, "1004"));
        CompletableFuture<Void> cf2 = CompletableFuture.runAsync(()->receiveMessage(receivedMessages1, subscription1, destination, message));
        CompletableFuture<Void> cf3 = CompletableFuture.runAsync(()->receiveMessage(receivedMessages2, subscription2, destination, message));
        CompletableFuture<Void> cf4 = CompletableFuture.runAsync(()->receiveMessage(receivedMessages3, subscription3, destination, message));
        CompletableFuture.allOf(cf1, cf2, cf3, cf4).join();

        disconnectClient(clientConnection1, receivedMessages1, "1005");
        disconnectClient(clientConnection2, receivedMessages2, "1006");
        disconnectClient(clientConnection3, receivedMessages3, "1007");
    }

    @Test
    public void shouldCallMessageEndpointWithOutboundReceivers() {
        Deque<byte[]> receivedMessages1 = new LinkedList<>();
        Deque<byte[]> receivedHeartbeats1 = new LinkedList<>();
        WebSocketClientConnection clientConnection1 = createWebSocketConnection("client4", receivedMessages1, receivedHeartbeats1);
        Deque<byte[]> receivedMessages2 = new LinkedList<>();
        Deque<byte[]> receivedHeartbeats2 = new LinkedList<>();
        WebSocketClientConnection clientConnection2 = createWebSocketConnection("client5", receivedMessages2, receivedHeartbeats2);
        Deque<byte[]> receivedMessages3 = new LinkedList<>();
        Deque<byte[]> receivedHeartbeats3 = new LinkedList<>();
        WebSocketClientConnection clientConnection3 = createWebSocketConnection("client6", receivedMessages3, receivedHeartbeats3);

        connectClient(clientConnection1, receivedMessages1, receivedHeartbeats1);
        connectClient(clientConnection2, receivedMessages2, receivedHeartbeats2);
        connectClient(clientConnection3, receivedMessages3, receivedHeartbeats3);

        String subscription2 = "sub5";
        String subscription3 = "sub6";

        String sendDestination = "/messageEndpoint/helloAsync";
        String subscribeDestination = "/topic/helloAsync";
        String message = "\"World\"";
        String receivedMessage = "\"Hello World\"";

        subscribeClient(clientConnection2, receivedMessages2, subscription2, subscribeDestination, "2002");
        subscribeClient(clientConnection3, receivedMessages3, subscription3, subscribeDestination, "2003");

        CompletableFuture<Void> cf1 = CompletableFuture.runAsync(()-> sendMessage(clientConnection1, receivedMessages1, sendDestination, message, "2004"));
        CompletableFuture<Void> cf3 = CompletableFuture.runAsync(()-> receiveMessage(receivedMessages2, subscription2, subscribeDestination, receivedMessage));
        CompletableFuture<Void> cf4 = CompletableFuture.runAsync(()-> receiveMessage(receivedMessages3, subscription3, subscribeDestination, receivedMessage));
        CompletableFuture.allOf(cf1, cf3, cf4).join();

        disconnectClient(clientConnection1, receivedMessages1, "2005");
        disconnectClient(clientConnection2, receivedMessages2, "2006");
        disconnectClient(clientConnection3, receivedMessages3, "2007");
    }

    private void receiveMessage(Deque<byte[]> receivedMessages, String subId, String destination, String message) {
        final byte [] messageFrame = FrameTestUtils.messageFrame(destination, ".*", subId, message);
        Awaitility.await().atMost(AWAIT_AT_MOST).pollInterval(AWAIT_POLL_INTERVAL).until(() -> !receivedMessages.isEmpty()
                && Pattern.compile(new String(messageFrame)).matcher(new String(receivedMessages.peek())).matches());
        receivedMessages.removeFirst();
    }

    private void sendMessage(WebSocketClientConnection connection, Deque<byte[]> receivedMessages, String destination, String message, String receipt) {
        final byte [] sendFrame = FrameTestUtils.sendFrame(destination, "text/plain", receipt, message);
        connection.sendBinaryAndAwait(sendFrame);
        final byte [] receiptFrame = FrameTestUtils.receiptFrame(receipt);
        Awaitility.await().atMost(AWAIT_AT_MOST).pollInterval(AWAIT_POLL_INTERVAL).until(() -> !receivedMessages.isEmpty()
                && Arrays.equals(receiptFrame, receivedMessages.peek()));
        receivedMessages.removeFirst();
    }

    private void subscribeClient(WebSocketClientConnection connection, Deque<byte[]> receivedMessages, String subId, String destination, String receipt) {
        final byte [] subscribeFrame = FrameTestUtils.subscribeFrame(subId,  destination, receipt);
        final byte [] receiptFrame = FrameTestUtils.receiptFrame(receipt);
        connection.sendBinaryAndAwait(subscribeFrame);
        Awaitility.await().atMost(AWAIT_AT_MOST).pollInterval(AWAIT_POLL_INTERVAL).until(() -> !receivedMessages.isEmpty());
        byte [] first = receivedMessages.removeFirst();
        Assertions.assertArrayEquals(receiptFrame, first);
    }

    private void connectClient(WebSocketClientConnection connection, Deque<byte[]> receivedMessages, Deque<byte[]> receivedHeartbeats) {
        final byte[] connectFrame = FrameTestUtils.connectFrame("www.example.com", "1000,1000");
        final byte[] connectedFrame = FrameTestUtils.connectedFrame(".*", "1000,1000");
        heartbeatManager.register(connection);
        connection.sendBinaryAndAwait(connectFrame);
        Awaitility.await().atMost(AWAIT_AT_MOST).pollInterval(AWAIT_POLL_INTERVAL).until(() -> !receivedMessages.isEmpty());
        byte [] first = receivedMessages.removeFirst();
        Matcher matcher = Pattern.compile(new String(connectedFrame)).matcher(new String(first));
        Assertions.assertTrue(matcher.matches());
        Awaitility.await().atMost(AWAIT_AT_MOST).pollInterval(AWAIT_POLL_INTERVAL).until(() -> receivedHeartbeats.size() > 1);
    }

    private void disconnectClient(WebSocketClientConnection connection, Deque<byte[]> receivedMessages, String receipt) {
        heartbeatManager.unregister(connection);
        final byte [] disconnectFrame = FrameTestUtils.disconnectFrame(receipt);
        final byte [] receiptFrame = FrameTestUtils.receiptFrame(receipt);
        connection.sendBinaryAndAwait(disconnectFrame);
        Awaitility.await().atMost(AWAIT_AT_MOST).pollInterval(AWAIT_POLL_INTERVAL).until(() -> !receivedMessages.isEmpty());
        byte [] first = receivedMessages.removeFirst();
        Assertions.assertArrayEquals(receiptFrame, first);
    }

    private WebSocketClientConnection createWebSocketConnection(String clientId, Deque<byte[]> receivedMessages, Deque<byte[]> receivedHeartbeats) {
        return stompWebSocketClient.openAndConsume(clientId, (connection, message) -> {
            var messageBytes = message.getBytes();
            if (Arrays.equals(messageBytes, Buffer.buffer(FrameParser.EOL).getBytes())) {
                receivedHeartbeats.add(messageBytes);
            } else {
                receivedMessages.add(messageBytes);
            }
        });
    }
}
