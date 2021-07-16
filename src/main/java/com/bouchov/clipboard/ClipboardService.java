package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.Clipboard;
import com.bouchov.clipboard.entities.Content;
import org.springframework.web.socket.WebSocketSession;

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

    void connect(UUID device, WebSocketSession session);

    Optional<Clipboard> getClipboard(Account account);

    Optional<Clipboard> getClipboardByToken(String token);

    void deleteContents(Account account);

    void setContents(List<Content> contents);

    Optional<String> shareClipboard(Account account);
}
