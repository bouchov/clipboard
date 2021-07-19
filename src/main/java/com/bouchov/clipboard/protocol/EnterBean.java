package com.bouchov.clipboard.protocol;

import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 12:01
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class EnterBean {
    private UUID device;
    private UUID target;

    public EnterBean() {
    }

    public UUID getDevice() {
        return device;
    }

    public void setDevice(UUID device) {
        this.device = device;
    }

    public UUID getTarget() {
        return target;
    }

    public void setTarget(UUID target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "[EnterBean" +
                " device=" + device +
                ", target=" + target +
                ']';
    }
}
