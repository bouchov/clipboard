package com.bouchov.clipboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 11:58
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Component
public class SessionListener implements HttpSessionListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ClipboardService service;

    @Autowired
    public SessionListener(ClipboardService service) {
        this.service = service;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("create session");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Long id = (Long) se.getSession().getAttribute(SessionAttributes.USER_ID);
        log.info("destroy session of " + id);
        UUID device = (UUID) se.getSession().getAttribute(SessionAttributes.DEVICE);
        if (id != null) {
            service.unregisterDevice(id, device);
        }
    }
}
