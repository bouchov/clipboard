package com.bouchov.clipboard.protocol;

import com.bouchov.clipboard.entities.Content;
import com.bouchov.clipboard.entities.ContentType;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 11:06
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class ContentBean {
    private ContentType type;

    @JsonDeserialize(using = RawJsonDeserializer.class)
    @JsonRawValue
    private String data;

    private UUID source;

    public ContentBean() {
    }

    public ContentBean(Content content) {
        this.type = content.getType();
        this.data = content.getData();
        this.source = content.getSource();
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        this.type = type;
    }

    public UUID getSource() {
        return source;
    }

    public void setSource(UUID source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "[ContentBean" +
                " type=" + type +
                ", data=" + (data == null ? null : '\'' + data + '\'') +
                ", source=" + source +
                ']';
    }
}
