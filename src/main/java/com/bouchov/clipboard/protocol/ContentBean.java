package com.bouchov.clipboard.protocol;

import com.bouchov.clipboard.entities.Content;
import com.bouchov.clipboard.entities.ContentType;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 11:06
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class ContentBean {
    private ContentType type;
    private String data;

    public ContentBean() {
    }

    public ContentBean(Content content) {
        this.type = content.getType();
        this.data = content.getData();
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

    @Override
    public String toString() {
        return "[ContentBean" +
                " type=" + type +
                ", data=" + (data == null ? null : '\'' + data + '\'') +
                ']';
    }
}
