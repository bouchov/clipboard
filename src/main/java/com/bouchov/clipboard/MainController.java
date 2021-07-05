package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.*;
import com.bouchov.clipboard.protocol.AccountBean;
import com.bouchov.clipboard.protocol.ContentBean;
import com.bouchov.clipboard.protocol.DeviceBean;
import com.bouchov.clipboard.protocol.ResponseBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private final ResourceLoader resourceLoader;
    private final HttpSession session;

    @Autowired
    public MainController(AccountRepository accountRepository,
            DeviceRepository deviceRepository,
            ContentRepository contentRepository,
            ResourceLoader resourceLoader,
            HttpSession session) {
        this.accountRepository = accountRepository;
        this.deviceRepository = deviceRepository;
        this.contentRepository = contentRepository;
        this.resourceLoader = resourceLoader;
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
    public DeviceBean device(@RequestBody DeviceBean deviceBean) {
        checkAuthorization(session);
        Device device;
        String token = deviceBean.getToken();
        if (token != null && !token.isBlank()) {
            device = deviceRepository.findByToken(UUID.fromString(token))
                    .orElseThrow(() -> new UserNotFoundException(token));
        } else {
            String name = deviceBean.getName();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name required");
            }
            Account account = getUser(session, accountRepository).orElseThrow();
            if (deviceRepository.findByAccountAndName(account, name).isPresent()) {
                throw new UserAlreadyExistsException(name);
            }
            DeviceType deviceType = deviceBean.getType();
            if (deviceType == null) {
                deviceType = DeviceType.OTHER;
            }
            device = deviceRepository.save(new Device(name, deviceType, UUID.randomUUID(), account));
        }
        session.setAttribute(SessionAttributes.DEVICE_ID, device.getId());
        return new DeviceBean(device);
    }

    @GetMapping("/devices")
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
                                content.getType(),
                                content.getData()))
                ));
            }
            return beans;
        }
    }

    @GetMapping("/share/{contentId}")
    public ContentBean share(@PathVariable Long contentId) {
        checkAuthorization(session);
        Content sharedContent = contentRepository.findById(contentId)
                .orElseThrow(() -> new UserNotFoundException(contentId));
        if (sharedContent.getToken() == null) {
            sharedContent.setToken(generateToken());
            contentRepository.save(sharedContent);
        }
        return new ContentBean(sharedContent);
    }

    private String generateToken() {
        String token;
        do {
            token = IdGenerator.generate();
        } while (contentRepository.findByToken(token).isPresent());
        return token;
    }

    @GetMapping("/shared/{token}")
    @ResponseBody
    public String shared(@PathVariable(name="token") String token)
            throws IOException {
        Content sharedContent = contentRepository.findByToken(token).orElse(null);
        String text;
        if (sharedContent == null) {
            text = "Content expired";
        } else if (sharedContent.getType() == ContentType.CLIPBOARD) {
            text = sharedContent.getData();
        } else {
            text = "Content is not supported";
        }
        Resource resource = resourceLoader.getResource("classpath:templates/shared-text.html");
        String content = Files.readString(Paths.get(resource.getURI()), StandardCharsets.UTF_8);
        return content.replace("${content}", text);
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
