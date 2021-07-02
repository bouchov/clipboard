package com.bouchov.clipboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 11:58
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class SessionListener implements HttpSessionListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("create session");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Object id = se.getSession().getAttribute(SessionAttributes.USER_ID);
        log.info("destroy session of " + id);
    }
}
