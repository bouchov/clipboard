package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Clipboard;
import com.bouchov.clipboard.entities.Content;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 11:58
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public interface ClipboardService {
    void disconnect(WebSocketSession session);

    void connect(UUID device, UUID target, WebSocketSession session);

    void sendMessage(UUID target, String message, WebSocketSession session);

    void sendBinaryMessageToPipe(ByteBuffer buffer, WebSocketSession session);

    Optional<Clipboard> getClipboard(Long accountId);

    Optional<Clipboard> getClipboardByToken(String token);

    void deleteContents(Long accountId);

    void setContents(List<Content> contents);

    Optional<String> shareClipboard(Long accountId);

    void unregisterDevice(Long accountId, UUID device);

    void registerDevice(Long accountId, UUID device);
}
