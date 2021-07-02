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
    @Column(nullable = false)
    private String data;
    @Column
    private ContentType type;

    public Content() {
    }

    public Content(Device source, String data, ContentType type) {
        this.source = source;
        this.data = data;
        this.type = type;
    }

    public Device getSource() {
        return source;
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
