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
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "[AccountBean" +
                " id=" + id +
                ", name=" + (name == null ? null : '\'' + name + '\'') +
                ", password=" + (password == null ? null : '\'' + password + '\'') +
                ']';
    }
}
