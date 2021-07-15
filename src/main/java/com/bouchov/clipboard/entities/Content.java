package com.bouchov.clipboard.entities;

import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:49
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class Content {
    private Long accountId;
    private ContentType type;
    private UUID source;
    private String token;
    private String data;

    public Content() {
    }

    public Content(Account account, ContentType type, UUID source, String data) {
        this.accountId = account.getId();
        this.type = type;
        this.source = source;
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getData() {
        return data;
    }

    public ContentType getType() {
        return type;
    }

    public UUID getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "[Content" +
                ", accountId=" + accountId +
                ", type=" + type +
                ", source=" + source +
                ", token=" + (token == null ? null : '\'' + token + '\'') +
                ", data=" + (data == null ? null : '\'' + data + '\'') +
                ']';
    }
}
