package com.bouchov.clipboard.protocol;

import com.bouchov.clipboard.entities.Account;

import java.util.List;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 11:13
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class ResponseBean {
    private AccountBean account;
    private UUID token;
    private List<ContentBean> contents;

    public ResponseBean() {
    }

    public ResponseBean(Account account) {
        this.account = new AccountBean(account);
    }

    public ResponseBean(Account account, UUID token) {
        this(account);
        this.token = token;
    }

    public AccountBean getAccount() {
        return account;
    }

    public void setAccount(AccountBean account) {
        this.account = account;
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public List<ContentBean> getContents() {
        return contents;
    }

    public void setContents(List<ContentBean> contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "[ResponseBean" +
                " account=" + account +
                ", token=" + token +
                ", contents=" + contents +
                ']';
    }
}
