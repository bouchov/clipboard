package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.*;
import com.bouchov.clipboard.protocol.AccountBean;
import com.bouchov.clipboard.protocol.ContentBean;
import com.bouchov.clipboard.protocol.LinkBean;
import com.bouchov.clipboard.protocol.ResponseBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ClipboardService service;
    private final ResourceLoader resourceLoader;
    private final HttpSession session;

    @Autowired
    public MainController(AccountRepository accountRepository,
            ClipboardService service,
            ResourceLoader resourceLoader,
            HttpSession session) {
        this.accountRepository = accountRepository;
        this.service = service;
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
            @RequestParam(name = "token", required = false) UUID token) {
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
        if (token == null) {
            token = UUID.randomUUID();
        }
        UUID theToken = token;
        session.setAttribute(SessionAttributes.DEVICE, theToken);
        ResponseBean bean = new ResponseBean(user, theToken);
        Optional<Clipboard> clipboard = service.getClipboard(user);
        clipboard.ifPresent(value -> bean.setContents(getContentsBean(theToken, value)));
        return bean;
    }

    private List<ContentBean> getContentsBean(UUID token, Clipboard clipboard) {
        List<Content> page = clipboard.getContents();
        return page.stream()
                .map(ContentBean::new)
                .collect(Collectors.toList());
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
        UUID token = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        ResponseBean bean = new ResponseBean(user, token);
        Optional<Clipboard> clipboard = service.getClipboard(user);
        clipboard.ifPresent(value -> bean.setContents(getContentsBean(token, value)));
        return bean;
    }

    @GetMapping("/clipboard")
    public List<ContentBean> getClipboard() {
        checkAuthorization(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        UUID token = (UUID) session.getAttribute(SessionAttributes.DEVICE);

        Optional<Clipboard> clipboard = service.getClipboard(account);
        if (clipboard.isPresent()) {
            return getContentsBean(token, clipboard.get());
        } else {
            return Collections.emptyList();
        }
    }

    @PostMapping("/clipboard")
    public List<ContentBean> setClipboard(
            @RequestBody List<ContentBean> contents) {
        checkAuthorization(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        UUID token = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        if (contents.isEmpty()) {
            service.deleteContents(account);
            return Collections.emptyList();
        } else {
            service.setContents(contents.stream()
                    .map(b -> new Content(account, b.getType(), token, b.getData()))
                    .collect(Collectors.toList()));
            Clipboard clipboard = service.getClipboard(account).orElseThrow();
            return getContentsBean(token, clipboard);
        }
    }

    @GetMapping("/share")
    public LinkBean share() {
        checkAuthorization(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        String token = service.shareClipboard(account)
                .orElseThrow(() -> new UserNotFoundException(account.getId()));
        return new LinkBean(token);
    }

    @GetMapping("/shared/{token}")
    @ResponseBody
    public String shared(@PathVariable(name="token") String token)
            throws IOException {
        Optional<Clipboard> clipboard = service.getClipboardByToken(token);
        String text;
        if (clipboard.isEmpty()) {
            text = "Content expired";
        } else if (clipboard.get().getContentType() == ContentType.CLIPBOARD) {
            text = clipboard.get().getContent().getData();
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
