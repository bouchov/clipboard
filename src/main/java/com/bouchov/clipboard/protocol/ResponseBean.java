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
    private UUID device;
    private List<ContentBean> contents;
    private Boolean welcome;
    private String errorMessage;
    private int errorCode;

    public ResponseBean() {
    }

    public ResponseBean(Boolean welcome) {
        this.welcome = welcome;
        errorCode = 0;
    }

    public ResponseBean(Account account) {
        this.account = new AccountBean(account);
        errorCode = 0;
    }

    public ResponseBean(Account account, UUID device) {
        this(account);
        this.device = device;
        errorCode = 0;
    }

    public ResponseBean(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public AccountBean getAccount() {
        return account;
    }

    public void setAccount(AccountBean account) {
        this.account = account;
    }

    public UUID getDevice() {
        return device;
    }

    public void setDevice(UUID device) {
        this.device = device;
    }

    public List<ContentBean> getContents() {
        return contents;
    }

    public void setContents(List<ContentBean> contents) {
        this.contents = contents;
    }

    public Boolean getWelcome() {
        return welcome;
    }

    public void setWelcome(Boolean welcome) {
        this.welcome = welcome;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "[ResponseBean" +
                " account=" + account +
                ", device=" + device +
                ", contents=" + contents +
                ", welcome=" + welcome +
                ']';
    }
}
