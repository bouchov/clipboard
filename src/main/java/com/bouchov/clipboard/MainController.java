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
        if (device == null) {
            device = UUID.randomUUID();
        }
        session.setAttribute(SessionAttributes.DEVICE, device);
        service.registerDevice(user, device);
        session.removeAttribute(SessionAttributes.TOKEN);
        ResponseBean bean = new ResponseBean(user, device);
        Optional<Clipboard> clipboard = service.getClipboard(user);
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
        Account user = accountRepository.findByName(name).orElse(null);
        if (user != null) {
            throw new UserAlreadyExistsException(name);
        }
        user = new Account(name, Password.create(password));
        user = accountRepository.save(user);

        session.setAttribute(SessionAttributes.USER_ID, user.getId());
        UUID device = UUID.randomUUID();
        session.setAttribute(SessionAttributes.DEVICE, device);
        service.registerDevice(user, device);
        session.removeAttribute(SessionAttributes.TOKEN);
        return new ResponseBean(user, device);
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
        ResponseBean bean;
        if (session.getAttribute(SessionAttributes.TOKEN) == null) {
            bean = new ResponseBean(user, token);
        } else {
            bean = new ResponseBean(token);
        }
        Optional<Clipboard> clipboard = service.getClipboard(user);
        clipboard.ifPresent(value -> bean.setContents(getContentsBean(value)));
        return bean;
    }

    @GetMapping("/clipboard")
    public List<ContentBean> getClipboard() {
        checkAuthorization(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        Optional<Clipboard> clipboard = service.getClipboard(account);
        if (clipboard.isPresent()) {
            return getContentsBean(clipboard.get());
        } else {
            return Collections.emptyList();
        }
    }

    @PostMapping("/clipboard")
    public List<ContentBean> setClipboard(
            @RequestBody List<ContentBean> contents) {
        checkAuthorization(session);
        checkIsNotAnonymous(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        UUID device = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        if (contents.isEmpty()) {
            service.deleteContents(account);
            return Collections.emptyList();
        } else {
            service.setContents(contents.stream()
                    .map(b -> new Content(account, b.getType(), device, b.getData()))
                    .collect(Collectors.toList()));
            Clipboard clipboard = service.getClipboard(account).orElseThrow();
            return getContentsBean(clipboard);
        }
    }

    @GetMapping("/share")
    public LinkBean share() {
        checkAuthorization(session);
        checkIsNotAnonymous(session);
        Account account = getUser(session, accountRepository).orElseThrow();
        UUID device = (UUID) session.getAttribute(SessionAttributes.DEVICE);
        Optional<Clipboard> clipboard = service.getClipboard(account);
        if (clipboard.isPresent() && clipboard.get().hasContent()) {
            if (Objects.equals(clipboard.get().getContent().getSource(), device)) {
                String token = service.shareClipboard(account)
                        .orElseThrow(() -> new UserNotFoundException(account.getId()));
                return new LinkBean(token);
            } else {
                throw new UserAlreadyExistsException("your are not the owner of content");
            }
        }
        throw new UserNotFoundException("empty clipboard");
    }

    @GetMapping("/shared/{token}")
    public RedirectView shared(@PathVariable(name="token") String token) {
        Optional<Clipboard> clipboard = service.getClipboardByToken(token);
        if (clipboard.isPresent()) {
            if (session.getAttribute(SessionAttributes.TOKEN) == null) {
                if (session.getAttribute(SessionAttributes.USER_ID) != null) {
                    //need re-load page
                    session.invalidate();
                } else {
                    Long accountId = clipboard.get().getAccountId();
                    Account account = accountRepository.findById(accountId).orElseThrow();
                    session.setAttribute(SessionAttributes.USER_ID, account.getId());
                    UUID device = UUID.randomUUID();
                    session.setAttribute(SessionAttributes.DEVICE, device);
                    session.setAttribute(SessionAttributes.TOKEN, token);
                    service.registerDevice(account, device);
                }
            }
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
}
