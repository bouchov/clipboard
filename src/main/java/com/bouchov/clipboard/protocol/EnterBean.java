package com.bouchov.clipboard.protocol;

import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 12:01
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class EnterBean {
    private UUID token;

    public EnterBean() {
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "[EnterBean" +
                " token=" + token +
                ']';
    }
}
