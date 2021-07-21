package com.bouchov.clipboard.protocol;

import com.bouchov.clipboard.entities.Account;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 18:40
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class AccountBean {
    private Long id;
    private String name;

    public AccountBean(Account account) {
        this.id = account.getId();
        this.name = account.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "[AccountBean" +
                " id=" + id +
                ", name=" + (name == null ? null : '\'' + name + '\'') +
                ']';
    }
}
