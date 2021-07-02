package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.*;
import com.bouchov.clipboard.protocol.AccountBean;
import com.bouchov.clipboard.protocol.ContentBean;
import com.bouchov.clipboard.protocol.DeviceBean;
import com.bouchov.clipboard.protocol.ResponseBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 18:33
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@RestController
@RequestMapping("/")
public class MainController extends AbstractController {
    private final AccountRepository accountRepository;
    private final DeviceRepository deviceRepository;
    private final ContentRepository contentRepository;
    private final HttpSession session;

    @Autowired
    public MainController(AccountRepository accountRepository,
            DeviceRepository deviceRepository,
            ContentRepository contentRepository,
            HttpSession session) {
        this.accountRepository = accountRepository;
        this.deviceRepository = deviceRepository;
        this.contentRepository = contentRepository;
        this.session = session;
    }

    @GetMapping
    public ModelAndView get() {
        return new ModelAndView("redirect:/index.html");
    }

    @PostMapping
    public ResponseBean login(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "token", required = false) String token) {
        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        Account user = accountRepository.findByName(name)
                .orElseThrow(() -> new UserNotFoundException(name));
        if (userId != null && !Objects.equals(user.getId(), userId)) {
            session.invalidate();
            throw new NeedReLoginException();
        }
        if (!Password.isEqual(user.getPassword(), password)) {
            throw new UserNotFoundException(name);
        }
        session.setAttribute(SessionAttributes.USER_ID, user.getId());
        Device device = null;
        if (token != null && !token.isBlank()) {
            device = deviceRepository.findByToken(UUID.fromString(token)).orElseThrow();
            session.setAttribute(SessionAttributes.DEVICE_ID, device.getId());
        }
        ResponseBean bean = new ResponseBean(user, device);
        Page<Content> page = contentRepository.findAllByAccount(user, Pageable.unpaged());
        if (page.hasContent()) {
            bean.setContents(page.map(ContentBean::new).getContent());
        }
        return bean;
    }

    @PostMapping("/device")
    public DeviceBean device(@RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "token", required = false) String token) {
        checkAuthorization(session);
        Device device;
        if (token != null && !token.isBlank()) {
            device = deviceRepository.findByToken(UUID.fromString(token))
                    .orElseThrow(() -> new UserNotFoundException(token));
        } else {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name required");
            }
            Account account = getUser(session, accountRepository).orElseThrow();
            if (deviceRepository.findByAccountAndName(account, name).isPresent()) {
                throw new UserAlreadyExistsException(name);
            }
            DeviceType deviceType;
            if (type == null || type.isBlank()) {
                deviceType = DeviceType.OTHER;
            } else {
                deviceType = DeviceType.valueOf(type);
            }
            device = deviceRepository.save(new Device(name, deviceType, UUID.randomUUID(), account));
        }
        session.setAttribute(SessionAttributes.DEVICE_ID, device.getId());
        return new DeviceBean(device);
    }

    @GetMapping("devices")
    public List<DeviceBean> devices() {
        checkAuthorization(session);
        Page<Device> page = deviceRepository.findAllByAccount(getUser(session, accountRepository).orElseThrow(),
                Pageable.unpaged());
        return page.map(DeviceBean::new).getContent();
    }

    @PostMapping("/register")
    public AccountBean register(
            @RequestParam String name,
            @RequestParam String password) {
        Account user = accountRepository.findByName(name).orElse(null);
        if (user != null) {
            throw new UserAlreadyExistsException(name);
        }
        user = new Account(name, Password.create(password));
        user = accountRepository.save(user);

        session.setAttribute(SessionAttributes.USER_ID, user.getId());
        return new AccountBean(user);
    }

    @RequestMapping("/ping")
    public ResponseBean ping() {
        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId == null) {
            throw new UserNotFoundException("session expired");
        }
        Account user = accountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Device device = null;
        Long deviceId = (Long) session.getAttribute(SessionAttributes.DEVICE_ID);
        if (deviceId != null) {
            device = deviceRepository.findById(deviceId).orElseThrow(() -> new UserNotFoundException(deviceId));
        }
        return new ResponseBean(user, device);
    }

    @GetMapping("/clipboard")
    public List<ContentBean> getClipboard() {
        checkAuthorization(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        Page<Content> page = contentRepository.findAllByAccount(account, Pageable.unpaged());
        return page.map(ContentBean::new).getContent();
    }

    @PostMapping("/clipboard")
    public List<ContentBean> setClipboard(
            @RequestBody List<ContentBean> contents) {
        checkAuthorization(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        if (contents.isEmpty()) {
            contentRepository.deleteAllByAccount(account);
            return Collections.emptyList();
        } else {
            ArrayList<ContentBean> beans = new ArrayList<>();
            Long deviceId = (Long) session.getAttribute(SessionAttributes.DEVICE_ID);
            Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new UserNotFoundException(deviceId));
            for (ContentBean content : contents) {
                beans.add(new ContentBean(contentRepository.save(
                        new Content(device,
                                content.getData(),
                                content.getType()))
                ));
            }
            return beans;
        }
    }


    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class UserNotFoundException extends RuntimeException {

        public UserNotFoundException(String login) {
            super("could not find user '" + login + "'.");
        }

        public UserNotFoundException(Long userId) {
            super("could not find user " + userId + ".");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    static class UserAlreadyExistsException extends RuntimeException {

        public UserAlreadyExistsException(String login) {
            super("user '" + login + "' already exists.");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    static class NeedReLoginException extends RuntimeException {
        public NeedReLoginException() {
            super("user is logged in with another credentials");
        }
    }
}
