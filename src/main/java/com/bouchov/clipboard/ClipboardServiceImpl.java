package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 11:59
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Service
public class ClipboardServiceImpl
        implements ClipboardService,
        DisposableBean,
        InitializingBean {
    private static final Object PRESENT = new Object();
    private final Logger log = LoggerFactory.getLogger(ClipboardService.class);

    private final Map<String, Container> tokens;
    private final Map<Long, Container> contents;
    private final AccountRepository accountRepository;
    private final ThreadPoolTaskScheduler quizScheduler;

    @Autowired
    public ClipboardServiceImpl(
            AccountRepository accountRepository,
            ThreadPoolTaskScheduler quizScheduler) {
        this.accountRepository = accountRepository;
        this.quizScheduler = quizScheduler;

        this.tokens = new ConcurrentHashMap<>();
        this.contents = new ConcurrentHashMap<>();
    }

    @Override
    public void disconnect(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(SessionAttributes.USER_ID);
        UUID token = (UUID) session.getAttributes().get(SessionAttributes.TOKEN);
        Optional<Account> optional = accountRepository.findById(userId);
        if (optional.isPresent()) {
            log.debug("disconnect: {}, token={}", optional.get().getName(), token);
        }
    }

    @Override
    public void connect(UUID token, WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(SessionAttributes.USER_ID);
        Account account = accountRepository.findById(userId).orElseThrow();
        List<String> strings = session.getHandshakeHeaders().get("User-Agent");
        String ua = "Unknown/0.0 (Unknown; No)";
        if (strings != null && !strings.isEmpty()) {
            ua = strings.get(0);
        }
        log.debug("received connection: {}, token={}}, UserAgent[{}]", account.getName(), token, ua);
    }

    @Override
    public void destroy() {
        log.info("DESTROYED");
        contents.clear();
        tokens.clear();
    }

    @Override
    public void afterPropertiesSet() {
        log.info("INITIALIZED");
    }

    @Override
    public Optional<Clipboard> getClipboard(Account account) {
        return getContents(contents.get(account.getId()));
    }

    private Optional<Clipboard> getContents(Container container) {
        if (container != null) {
            return Optional.of(container.clipboard);
        }
        return Optional.empty();
    }

    @Override
    public void deleteContents(Account account) {
        Container container = contents.remove(account.getId());
        if (container != null && container.token != null) {
            tokens.remove(container.token);
        }
    }

    @Override
    public void setContents(List<Content> contents) {
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("empty contents");
        }
        Content first = contents.get(0);
        Long accountId = first.getAccountId();
        AtomicReference<ContentType> type = new AtomicReference<>(first.getType());
        if (contents.size() > 1) {
            if (contents.stream().anyMatch(c -> c.getType() == ContentType.CLIPBOARD)) {
                throw new IllegalArgumentException("only one element of type clipboard is allowed");
            }
            contents.forEach(c -> {
                if (type.get() != c.getType()) {
                    type.set(ContentType.BINARY);
                }
            });
        }
        this.contents.put(accountId, new Container(accountId, new Clipboard(accountId, type.get(), contents)));
    }

    @Override
    public Optional<String> shareClipboard(Account account) {
        Container container = contents.get(account.getId());
        if (container != null) {
            if (container.token == null) {
                generateToken(container);
            }
            return Optional.of(container.token);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Clipboard> getClipboardByToken(String token) {
        return getContents(tokens.get(token));
    }

    private void generateToken(Container container) {
        String token;
        do {
            token = IdGenerator.generate();
            if (!tokens.containsKey(token)) {
                tokens.computeIfAbsent(token, (k) -> {
                    container.token = k;
                    return container;
                });
            }
        } while (container.token == null);
    }

    private static class Container {
        volatile String token;
        final Long accountId;
        final Clipboard clipboard;

        public Container(Long accountId, Clipboard clipboard) {
            this.accountId = accountId;
            this.clipboard = clipboard;
        }
    }
}
