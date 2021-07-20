package com.bouchov.clipboard.entities;

import java.util.List;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 15:49
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class Clipboard {
    private final Long accountId;
    private final ContentType contentType;
    private final List<Content> contents;

    public Clipboard(Long accountId, ContentType contentType, List<Content> contents) {
        this.accountId = accountId;
        this.contentType = contentType;
        this.contents = contents;
    }

    public Long getAccountId() {
        return accountId;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public List<Content> getContents() {
        return contents;
    }

    public Content getContent() {
        return contents.get(0);
    }

    public boolean hasContent() {
        return contents != null && !contents.isEmpty();
    }
}
