package com.bouchov.clipboard.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:58
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public interface ContentRepository extends CrudRepository<Content,Long> {
    @Query("select C from Content C where C.source.account = :account")
    Page<Content> findAllByAccount(@Param("account") Account account, Pageable pageable);

    @Query("delete from Content C where C.source.account = :account")
    long deleteAllByAccount(@Param("account") Account account);
}
