package com.bouchov.clipboard.protocol;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 11:57
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class RequestBean {
    private EnterBean enter;

    public RequestBean() {
    }

    public EnterBean getEnter() {
        return enter;
    }

    public void setEnter(EnterBean enter) {
        this.enter = enter;
    }
}
