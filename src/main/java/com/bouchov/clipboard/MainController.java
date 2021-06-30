package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.AccountRepository;
import com.bouchov.clipboard.entities.Password;
import com.bouchov.clipboard.protocol.AccountBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 18:33
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@RestController
@RequestMapping("/")
public class MainController {
    private final AccountRepository accountRepository;
    private final HttpSession session;

    @Autowired
    public MainController(AccountRepository accountRepository,
            HttpSession session) {
        this.accountRepository = accountRepository;
        this.session = session;
    }

    @GetMapping
    public ModelAndView get() {
        return new ModelAndView("redirect:/index.html");
    }

    @PostMapping
    public AccountBean login(
            @RequestParam String name,
            @RequestParam String password) {
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
        return new AccountBean(user);
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
    public AccountBean ping() {
        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId == null) {
            throw new UserNotFoundException("session expired");
        }
        Account user = accountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return new AccountBean(user);
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
