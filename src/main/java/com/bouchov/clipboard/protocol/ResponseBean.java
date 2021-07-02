package com.bouchov.clipboard.protocol;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.Device;

import java.util.List;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 11:13
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class ResponseBean {
    private AccountBean account;
    private DeviceBean device;
    private List<ContentBean> contents;

    public ResponseBean() {
    }

    public ResponseBean(Account account) {
        this.account = new AccountBean(account);
    }

    public ResponseBean(Account account, Device device) {
        this(account);
        if (device != null) {
            this.device = new DeviceBean(device);
        }
    }

    public AccountBean getAccount() {
        return account;
    }

    public void setAccount(AccountBean account) {
        this.account = account;
    }

    public DeviceBean getDevice() {
        return device;
    }

    public void setDevice(DeviceBean device) {
        this.device = device;
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
                ", device=" + device +
                ", contents=" + contents +
                ']';
    }
}
