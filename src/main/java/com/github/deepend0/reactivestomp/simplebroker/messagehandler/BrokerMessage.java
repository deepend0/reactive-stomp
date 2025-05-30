package com.github.deepend0.reactivestomp.simplebroker.messagehandler;

public abstract class BrokerMessage {
    private final String subscriberId;

    public BrokerMessage(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getSubscriberId() {
        return subscriberId;
    }
}
