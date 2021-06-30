package com.bouchov.clipboard.entities;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:48
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public interface AccountRepository extends CrudRepository<Account,Long> {
    Optional<Account> findByName(String name);
}
