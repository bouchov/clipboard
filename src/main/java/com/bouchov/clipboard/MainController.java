package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.*;
import com.bouchov.clipboard.protocol.ContentBean;
import com.bouchov.clipboard.protocol.LinkBean;
import com.bouchov.clipboard.protocol.ResponseBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
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
    private final HttpSession session;

    @Autowired
    public MainController(AccountRepository accountRepository,
            ClipboardService service,
            HttpSession session) {
        this.accountRepository = accountRepository;
        this.service = service;
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
            @RequestParam(name = "device", required = false) UUID device) {
        Long accountId = (Long) session.getAttribute(SessionAttributes.ACCOUNT);
        Account account = accountRepository.findByName(name)
                .orElseThrow(() -> new UserNotFoundException(name));
        if (accountId != null && !Objects.equals(account.getId(), accountId)) {
            session.invalidate();
            throw new NeedReLoginException();
        }
        if (!Password.isEqual(account.getPassword(), password)) {
            throw new UserNotFoundException(name);
        }
        accountId = account.getId();
        session.setAttribute(SessionAttributes.ACCOUNT, accountId);
        if (device == null) {
            device = UUID.randomUUID();
        }
        session.setAttribute(SessionAttributes.DEVICE, device);
        service.registerDevice(accountId, device);
        session.removeAttribute(SessionAttributes.TOKEN);
        ResponseBean bean = new ResponseBean(account, device);
        Optional<Clipboard> clipboard = service.getClipboard(accountId);
        clipboard.ifPresent(value -> bean.setContents(getContentsBean(value)));
        return bean;
    }

    private List<ContentBean> getContentsBean(Clipboard clipboard) {
        List<Content> page = clipboard.getContents();
        return page.stream()
                .map(ContentBean::new)
                .collect(Collectors.toList());
    }

    @PostMapping("/register")
    public ResponseBean register(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "password") String password) {
        Account account = accountRepository.findByName(name).orElse(null);
        if (account != null) {
            throw new UserAlreadyExistsException(name);
        }
        account = new Account(name, Password.create(password));
        account = accountRepository.save(account);

        session.setAttribute(SessionAttributes.ACCOUNT, account.getId());
        UUID device = UUID.randomUUID();
        session.setAttribute(SessionAttributes.DEVICE, device);
        service.registerDevice(account.getId(), device);
        session.removeAttribute(SessionAttributes.TOKEN);
        return new ResponseBean(account, device);
    }

    @RequestMapping("/ping")
    public ResponseBean ping() {
        Long accountId = (Long) session.getAttribute(SessionAttributes.ACCOUNT);
        if (accountId == null) {
            throw new UserNotFoundException("session expired");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new UserNotFoundException(accountId));
        UUID device = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        ResponseBean bean;
        if (session.getAttribute(SessionAttributes.TOKEN) == null) {
            bean = new ResponseBean(account, device);
        } else {
            bean = new ResponseBean(device);
        }
        Optional<Clipboard> clipboard = service.getClipboard(accountId);
        clipboard.ifPresent(value -> bean.setContents(getContentsBean(value)));
        return bean;
    }

    @GetMapping("/clipboard")
    public List<ContentBean> getClipboard() {
        Long accountId = checkAuthorization(session);
        Optional<Clipboard> clipboard = service.getClipboard(accountId);
        if (clipboard.isPresent()) {
            return getContentsBean(clipboard.get());
        } else {
            return Collections.emptyList();
        }
    }

    @PostMapping("/clipboard")
    public List<ContentBean> setClipboard(
            @RequestBody List<ContentBean> contents) {
        Long accountId = checkAuthorization(session);
        checkIsNotAnonymous(session);
        UUID device = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        if (contents.isEmpty()) {
            service.deleteContents(accountId);
            return Collections.emptyList();
        } else {
            service.setContents(contents.stream()
                    .map(b -> new Content(accountId, b.getType(), device, b.getData()))
                    .collect(Collectors.toList()));
            Clipboard clipboard = service.getClipboard(accountId).orElseThrow();
            return getContentsBean(clipboard);
        }
    }

    @PostMapping("/share")
    public LinkBean share() {
        Long accountId = checkAuthorization(session);
        checkIsNotAnonymous(session);
        UUID device = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        Optional<Clipboard> clipboard = service.getClipboard(accountId);
        if (clipboard.isPresent() && clipboard.get().hasContent()) {
            if (Objects.equals(clipboard.get().getContent().getSource(), device)) {
                String token = service.shareClipboard(accountId)
                        .orElseThrow(() -> new UserNotFoundException(accountId));
                return new LinkBean(token);
            } else {
                throw new UserAlreadyExistsException("your are not the owner of content");
            }
        }
        throw new UserNotFoundException("empty clipboard");
    }

    @GetMapping("/share/{token}")
    public RedirectView shared(@PathVariable(name="token") String token) {
        Long accountId = (Long) session.getAttribute(SessionAttributes.ACCOUNT);
        if (accountId != null) {
            Object oldToken = session.getAttribute(SessionAttributes.TOKEN);
            if (!Objects.equals(token, oldToken)) {
                session.invalidate();
                return new RedirectView("/share/" + token);
            }
        }
        Optional<Clipboard> clipboard = service.getClipboardByToken(token);
        if (clipboard.isPresent()) {
            if (accountId == null) {
                accountId = clipboard.get().getAccountId();
                session.setAttribute(SessionAttributes.ACCOUNT, accountId);
                UUID device = UUID.randomUUID();
                session.setAttribute(SessionAttributes.DEVICE, device);
                session.setAttribute(SessionAttributes.TOKEN, token);
                service.registerDevice(accountId, device);
            }
        } else {
            session.invalidate();
            throw new ContentExpiredException(token);
        }
        return new RedirectView("/");
    }

    @GetMapping("/logout")
    public void logout() {
        session.invalidate();
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

    @ResponseStatus(HttpStatus.GONE)
    static class ContentExpiredException extends RuntimeException {
        public ContentExpiredException(String token) {
            super("content is expired " + token);
        }
    }
}
