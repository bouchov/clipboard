package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Alexandre Y. Bouchov
 * Date: 02.07.2021
 * Time: 10:17
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class AbstractController {
    protected Long checkAuthorization(HttpSession session) {
        Long userId = (Long) session.getAttribute(SessionAttributes.ACCOUNT);
        if (userId == null) {
            throw new AuthorizationRequiredException();
        }
        return userId;
    }

    protected void checkIsNotAnonymous(HttpSession session) {
        if (session.getAttribute(SessionAttributes.TOKEN) != null) {
            throw new AuthorizationRequiredException("anonymous access denied");
        }
    }

    protected Optional<Account> getUser(HttpSession session, AccountRepository repository) {
        Long userId = (Long) session.getAttribute(SessionAttributes.ACCOUNT);
        if (userId == null) {
            return Optional.empty();
        }
        return repository.findById(userId);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class AuthorizationRequiredException extends RuntimeException {
        public AuthorizationRequiredException() {
        }

        public AuthorizationRequiredException(String message) {
            super(message);
        }
    }
}
