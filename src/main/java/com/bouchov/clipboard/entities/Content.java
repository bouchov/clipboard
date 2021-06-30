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
    private String name;
    @Column
    private ContentType type;

    public Content() {
    }

    public Content(Device source, String name, ContentType type) {
        this.source = source;
        this.name = name;
        this.type = type;
    }

    public Device getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    public ContentType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "[Content super=" + super.toString() +
                ", source=" + source +
                ", name=" + (name == null ? null : '\'' + name + '\'') +
                ", type=" + type +
                ']';
    }
}
