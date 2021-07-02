package com.bouchov.clipboard.entities;

import javax.persistence.*;
import java.util.UUID;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 16:55
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Entity
@Table(name = "device",
        uniqueConstraints = {
        @UniqueConstraint(name = "device_uk",
                columnNames = {"account_id", "name"})
})
public class Device extends BasicEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private DeviceType type;

    @Column
    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="account_id", nullable=false, updatable=false)
    private Account account;

    public Device() {
    }

    public Device(String name, DeviceType type, UUID token, Account account) {
        this.name = name;
        this.type = type;
        this.token = token;
        this.account = account;
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

    public Account getAccount() {
        return account;
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
