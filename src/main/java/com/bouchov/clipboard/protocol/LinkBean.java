package com.bouchov.clipboard.protocol;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 15:45
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class LinkBean {
    String token;

    public LinkBean(String token) {
        this.token = token;
    }

    public LinkBean() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "[LinkBean" +
                " token=" + (token == null ? null : '\'' + token + '\'') +
                ']';
    }
}
