package com.bouchov.clipboard.protocol;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 11:57
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class RequestBean {
    private EnterBean enter;
    private UUID recipient;
    @JsonDeserialize(using = RawJsonDeserializer.class)
    @JsonRawValue
    private String message;

    public RequestBean() {
    }

    public EnterBean getEnter() {
        return enter;
    }

    public void setEnter(EnterBean enter) {
        this.enter = enter;
    }

    public UUID getRecipient() {
        return recipient;
    }

    public void setRecipient(UUID recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "[RequestBean" +
                " enter=" + enter +
                ", recipient=" + recipient +
                ", message=" + (message == null ? null : '\'' + message + '\'') +
                ']';
    }
}
