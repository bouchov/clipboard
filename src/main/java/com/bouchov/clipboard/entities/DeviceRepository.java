package com.bouchov.clipboard.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 18:48
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public interface DeviceRepository extends CrudRepository<Device,Long> {
    Optional<Device> findByToken(UUID token);

    Optional<Device> findByAccountAndName(Account account, String name);

    Page<Device> findAllByAccount(Account account, Pageable pageable);
}
