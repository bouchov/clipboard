package com.bouchov.clipboard;

import com.bouchov.clipboard.protocol.EnterBean;
import com.bouchov.clipboard.protocol.RequestBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 11:53
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Component
public class WebSocketHandler extends AbstractWebSocketHandler {
    private static final CloseStatus SESSION_EXPIRED = new CloseStatus(4004, "session expired");
    private static final CloseStatus INVALID_DEVICE = new CloseStatus(4001, "invalid device");

    private final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    @Autowired
    private ClipboardService service;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        if (accountId == null) {
            log.debug("received anonymous connection: close");
            session.close(SESSION_EXPIRED);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        if (accountId != null) {
            service.disconnect(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        if (accountId == null) {
            log.debug("received message from anonymous connection: close");
            session.close(SESSION_EXPIRED);
            return;
        }
        RequestBean request = new ObjectMapper().readValue(message.getPayload(), RequestBean.class);
        log.debug("received message: {}", request);
        if (request.getEnter() != null) {
            EnterBean enter = request.getEnter();
            UUID device = enter.getDevice();
            UUID deviceFromSession = (UUID) session.getAttributes().get(SessionAttributes.DEVICE);
            if (!Objects.equals(device, deviceFromSession)) {
                log.debug("different devices: {} and {}", device, deviceFromSession);
            }
            try {
                service.connect(device, enter.getTarget(), session);
            } catch (Exception e) {
                log.warn("error connecting client", e);
                session.close(INVALID_DEVICE);
            }
        } else if (request.getRecipient() != null) {
            service.sendMessage(request.getRecipient(), request.getMessage(), session);
        } else {
            log.warn("unsupported message");
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
            throws IOException {
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        if (accountId != null) {
            log.debug("received binary message: {}", message.getPayloadLength());
            service.sendBinaryMessageToPipe(message.getPayload(), session);
        } else {
            log.debug("received binary message from anonymous connection: close");
            session.close(SESSION_EXPIRED);
        }
    }
}
