package com.bouchov.clipboard.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:49
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Entity
public class Content extends BasicEntity {
    @ManyToOne(optional = false)
    private Device source;
    @Column
    private ContentType type;
    @Column(unique = true)
    private String token;
    @Column(nullable = false)
    private String data;

    public Content() {
    }

    public Content(Device source, ContentType type, String data) {
        this.source = source;
        this.type = type;
        this.data = data;
    }

    public Device getSource() {
        return source;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getData() {
        return data;
    }

    public ContentType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "[Content super=" + super.toString() +
                ", source=" + source +
                ", type=" + type +
                ", data=" + (data == null ? null : '\'' + data + '\'') +
                ']';
    }
}
