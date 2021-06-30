package com.bouchov.clipboard.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 16:55
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Entity
public class Device extends BasicEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private DeviceType type;

    @Column
    private UUID token;

    @ManyToOne
    private Account account;

    public Device() {
    }

    public Device(String name, DeviceType type, UUID token) {
        this.name = name;
        this.type = type;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public DeviceType getType() {
        return type;
    }

    public UUID getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "[Device super=" + super.toString() +
                ", name=" + (name == null ? null : '\'' + name + '\'') +
                ", type=" + type +
                ", token=" + token +
                ']';
    }
}
