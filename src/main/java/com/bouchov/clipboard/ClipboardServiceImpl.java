package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.*;
import com.bouchov.clipboard.protocol.ResponseBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
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
    private final Logger log = LoggerFactory.getLogger(ClipboardService.class);

    @Value("${clipboard.gc.period:PT1H}")
    private Duration gcPeriod;
    @Value("${clipboard.gc.expire:P1D}")
    private Duration expiredThreshold;
    private volatile ScheduledFuture<?> gcTask;

    private final Map<String, Container> tokens;
    private final Map<Long, Container> contents;
    private final AccountRepository accountRepository;
    private final ThreadPoolTaskScheduler scheduler;

    @Autowired
    public ClipboardServiceImpl(
            AccountRepository accountRepository,
            ThreadPoolTaskScheduler scheduler) {
        this.accountRepository = accountRepository;
        this.scheduler = scheduler;

        this.tokens = new ConcurrentHashMap<>();
        this.contents = new ConcurrentHashMap<>();
    }

    @Override
    public void registerDevice(Long accountId, UUID device) {
        log.debug("registerDevice: {}, device={}", accountId, device);
        Container container = contents.get(accountId);
        if (container == null) {
            container = contents.computeIfAbsent(accountId, (id) -> new Container(id, device, null));
        }
        container.addDevice(device);
    }

    @Override
    public void unregisterDevice(Long accountId, UUID device) {
        log.debug("unregisterDevice: {}, device={}", accountId, device);
        Container container = contents.get(accountId);
        if (container != null) {
            container.removeDevice(device);
            AtomicReference<String> oldToken = new AtomicReference<>();
            container = contents.compute(accountId, (id, c) -> {
                if (c != null) {
                    if (c.isEmpty() || Objects.equals(device, c.owner)) {
                        oldToken.set(c.token);
                        return null;
                    }
                }

                return c;
            });
            if (oldToken.get() != null) {
                tokens.remove(oldToken.get());
            }
            if (container == null) {
                log.debug("remove clipboard of {}", accountId);
            }
        }
    }

    @Override
    public void disconnect(WebSocketSession session) {
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        UUID device = (UUID) session.getAttributes().get(SessionAttributes.DEVICE);
        log.debug("disconnect: {}, device={}", accountId, device);
        Container container = contents.get(accountId);
        if (container != null) {
            container.removeConnection(device);
        }
    }

    @Override
    public void connect(UUID device, UUID target, WebSocketSession session) {
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        List<String> strings = session.getHandshakeHeaders().get("User-Agent");
        String ua = "Unknown/0.0 (Unknown; No)";
        if (strings != null && !strings.isEmpty()) {
            ua = strings.get(0);
        }
        log.debug("received connection: {}, device={}, UserAgent[{}]", accountId, device, ua);
        Container container = contents.get(accountId);
        if (container == null) {
            container = contents.computeIfAbsent(accountId, (id) -> new Container(id, device, null));
        }
        if (target != null) {
            Optional<WebSocketSession> connection = container.getConnection(target);
            if (connection.isPresent()) {
                UUID oldTarget = container.target;
                if (oldTarget == null) {
                    log.debug("register binary data pipe " + target + "->" + device);
                    container.target = device;
                } else if (!Objects.equals(oldTarget, device)) {
                    throw new IllegalStateException("cannot register new pipe, already exists " + target + "->" + oldTarget);
                }
            } else {
                log.warn("connect: unknown target: {} of user {}", target, accountId);
                throw new IllegalStateException("unknown device: " + target + " of user " + accountId);
            }

        }
        container.addConnection(device, session);
        sendMessage(accountId, session, asMessage(new ResponseBean(true)));
    }

    @Override
    public void sendMessage(UUID target, String message, WebSocketSession session) {
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        Container container = contents.get(accountId);
        if (container == null) {
            throw new IllegalStateException("cannot find clipboard of user " + accountId);
        }

        Optional<WebSocketSession> connection = container.getConnection(target);
        if (connection.isPresent()) {
            WebSocketSession dest = connection.get();
            sendMessage(accountId, dest, asMessage(message));
        } else {
            log.warn("sendMessage: unknown device: " + target + " of user " + accountId);
            sendMessage(accountId, session, asMessage(new ResponseBean(4, "Target is gone")));
            throw new IllegalStateException("unknown device: " + target + " of user " + accountId);
        }
    }

    @Override
    public void sendBinaryMessageToPipe(ByteBuffer buffer, WebSocketSession session) {
        Long accountId = (Long) session.getAttributes().get(SessionAttributes.ACCOUNT);
        UUID device = (UUID) session.getAttributes().get(SessionAttributes.DEVICE);
        Container container = contents.get(accountId);
        if (container == null) {
            throw new IllegalStateException("cannot find clipboard of user " + accountId);
        }
        if (Objects.equals(container.owner, device)) {
            UUID target = container.target;
            Optional<WebSocketSession> connection = container.getConnection(target);
            if (connection.isPresent()) {
                sendMessage(accountId, connection.get(), asMessage(buffer));
            } else {
                sendMessage(accountId, session, asMessage(new ResponseBean(3, "pipe is not exist")));
                throw new IllegalStateException("cannot send pipe, not an owner " + device);
            }
        } else {
            sendMessage(accountId, session, asMessage(new ResponseBean(2, "not an owner")));
            throw new IllegalStateException("cannot send pipe, not an owner " + device);
        }
    }

    private static WebSocketMessage<?> asMessage(String message) {
        return new TextMessage(message);
    }

    private static WebSocketMessage<?> asMessage(ByteBuffer buffer) {
        return new BinaryMessage(buffer);
    }

    private static WebSocketMessage<?> asMessage(Object jsonBean) {
        try {
            return new TextMessage(new ObjectMapper().writeValueAsString(jsonBean));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void sendMessage(Long accountId, WebSocketSession session, WebSocketMessage<?> message) {
        try {
            session.sendMessage(message);
        } catch (IOException e) {
            log.warn("unable to send message to " + accountId, e);
        }
    }

    @Override
    public void destroy() {
        if (gcTask != null) {
            gcTask.cancel(true);
            gcTask = null;
        }
        contents.clear();
        tokens.clear();
        log.info("DESTROYED");
    }

    @Override
    public void afterPropertiesSet() {
        scheduleGc(gcPeriod);
        log.info("INITIALIZED");
    }

    private void scheduleGc(Duration period) {
        gcTask = scheduler.scheduleAtFixedRate(this::gc, Instant.now().plusSeconds(60), period);
        log.debug("account gc scheduled at rate: " + period);
    }

    @Transactional
    public synchronized void gc() {
        log.debug("gc accounts");
        int pageSize = 100;
        Page<Account> page = accountRepository.findAllByLastLoginBefore(
                Date.from(Instant.now().minus(expiredThreshold)),
                Pageable.ofSize(pageSize));
        if (page.hasContent()) {
            page.get().forEach(accountRepository::delete);
            log.debug("gc: {} accounts deleted", page.getSize());
            if (page.hasNext()) {
                log.debug("gc: schedule in 1 sec");
                scheduler.schedule(this::gc, Instant.now().plusSeconds(1L));
            }
        } else {
            log.debug("gc: no expired accounts");
        }
    }

    @Override
    public Optional<Clipboard> getClipboard(Long accountId) {
        return getContents(contents.get(accountId));
    }

    private Optional<Clipboard> getContents(Container container) {
        if (container != null) {
            return Optional.ofNullable(container.clipboard);
        }
        return Optional.empty();
    }

    @Override
    public void deleteContents(Long accountId) {
        AtomicReference<String> oldToken = new AtomicReference<>();
        contents.computeIfPresent(accountId, (id, container) -> {
            oldToken.set(container.token);
            return container.copy(null, null);
        });
        if (oldToken.get() != null) {
            tokens.remove(oldToken.get());
        }
    }

    @Override
    public void setContents(List<Content> contents) {
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("empty contents");
        }
        Content first = contents.get(0);
        Long accountId = first.getAccountId();
        UUID owner = first.getSource();
        if (contents.size() > 1) {
            if (contents.stream().anyMatch(c -> c.getType() == ContentType.CLIPBOARD)) {
                throw new IllegalArgumentException("only one element of type clipboard is allowed");
            }
        }
        log.debug("setContents: " + accountId + ", " + contents);
        Clipboard clipboard = new Clipboard(accountId, first.getType(), contents);
        AtomicReference<String> oldToken = new AtomicReference<>();
        this.contents.compute(accountId, (id, container) -> {
            if (container == null) {
                return new Container(id, owner, clipboard);
            } else {
                oldToken.set(container.token);
                return container.copy(owner, clipboard);
            }
        });
        if (oldToken.get() != null) {
            tokens.remove(oldToken.get());
        }
    }

    @Override
    public Optional<String> shareClipboard(Long accountId) {
        Container container = contents.get(accountId);
        if (container != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (container) {
                if (container.token == null) {
                    generateToken(container);
                }
            }
            log.debug("user {} generated token '{}'", accountId, container.token);
            return Optional.of(container.token);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Clipboard> getClipboardByToken(String token) {
        Container container = tokens.get(token);
        if (container != null && Objects.equals(token, container.token)) {
            return getContents(container);
        }
        return Optional.empty();
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
        final Long accountId;
        final UUID owner;
        final Map<UUID, WebSocketSession> connections;
        final Set<UUID> devices;
        volatile String token;
        volatile UUID target;
        volatile Clipboard clipboard;

        public Container(Long accountId, UUID owner, Clipboard clipboard) {
            this.accountId = accountId;
            this.owner = owner;
            this.clipboard = clipboard;
            this.connections = new HashMap<>();
            this.devices = new HashSet<>();
        }

        public synchronized Container copy(UUID owner, Clipboard clipboard) {
            return new Container(this, owner, clipboard);
        }

        private Container(Container that, UUID owner, Clipboard clipboard) {
            this.accountId = that.accountId;
            this.owner = owner;
            this.token = null;
            this.target = null;
            this.clipboard = clipboard;
            this.connections = new HashMap<>(that.connections);
            this.devices = new HashSet<>(that.devices);
        }

        public synchronized void addDevice(UUID device) {
            devices.add(device);
        }

        public synchronized void removeDevice(UUID device) {
            if (devices.remove(device)) {
                removeConnection(device);
            }
        }

        public synchronized boolean isEmpty() {
            return devices.isEmpty();
        }

        public synchronized void addConnection(UUID device, WebSocketSession session) {
            if (devices.contains(device)) {
                connections.put(device, session);
            } else {
                throw new IllegalArgumentException("cannot accept connection from unknown device: " + device);
            }
        }

        public synchronized Optional<WebSocketSession> getConnection(UUID device) {
            return Optional.ofNullable(connections.get(device));
        }

        public synchronized void removeConnection(UUID device) {
            if (Objects.equals(target, device)) {
                target = null;
            }
            WebSocketSession session = connections.remove(device);
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    //nop
                }
            }
        }
    }
}
