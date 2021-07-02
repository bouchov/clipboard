package com.bouchov.clipboard.protocol;

import com.bouchov.clipboard.entities.Device;
import com.bouchov.clipboard.entities.DeviceType;

/**
 * Alexandre Y. Bouchov
 * Date: 01.07.2021
 * Time: 12:41
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class DeviceBean {
    private String name;
    private DeviceType type;
    private String token;

    public DeviceBean() {
    }

    public DeviceBean(Device device) {
        this.name = device.getName();
        this.type = device.getType();
        this.token = device.getToken().toString();
    }

    public DeviceBean(String name, String token) {
        this.name = name;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "[DeviceBean" +
                " name=" + (name == null ? null : '\'' + name + '\'') +
                ", type=" + type +
                ", token=" + (token == null ? null : '\'' + token + '\'') +
                ']';
    }
}
