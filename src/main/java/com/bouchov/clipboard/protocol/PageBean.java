package com.bouchov.clipboard.protocol;

import java.util.List;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 13:00
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class PageBean<V> {
    private int page;
    private int size;
    private int total;
    private List<V> elements;

    public PageBean(int page, int size, int total) {
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public static <O> PageBean<O> empty() {
        return new PageBean<>(0, 0, 0);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<V> getElements() {
        return elements;
    }

    public void setElements(List<V> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return "[Page " +
                "page=" + page +
                ", size=" + size +
                ", total=" + total +
                ", elements=" + elements +
                ']';
    }
}
