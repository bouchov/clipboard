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
    protected void checkAuthorization(HttpSession session) {
        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId == null) {
            throw new AuthorizationRequiredException();
        }
    }

    protected Optional<Account> getUser(HttpSession session, AccountRepository repository) {
        Long userId = (Long) session.getAttribute(SessionAttributes.USER_ID);
        if (userId == null) {
            return Optional.empty();
        }
        return repository.findById(userId);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class AuthorizationRequiredException extends RuntimeException {
        public AuthorizationRequiredException() {
        }
    }
}
