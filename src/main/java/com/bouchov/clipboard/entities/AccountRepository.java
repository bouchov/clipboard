package com.bouchov.clipboard.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.Optional;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:48
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public interface AccountRepository extends CrudRepository<Account,Long> {
    Optional<Account> findByName(String name);

    Page<Account> findAllByLastLoginBefore(Date expiration, Pageable pageable);
}
