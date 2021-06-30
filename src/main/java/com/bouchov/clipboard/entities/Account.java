package com.bouchov.clipboard.entities;

import com.fasterxml.jackson.core.JsonProcessingException;

import javax.persistence.*;
import java.util.List;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 16:54
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Entity
public class Account extends BasicEntity {
    @Column(nullable = false, unique = true)
    private String name;

    @Transient
    private Password password;
    @Column
    private String jsonPassword;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<Device> devices;

    public Account() {
    }

    public Account(String name, Password password) {
        this.name = name;
        this.password = password;
        try {
            this.jsonPassword = password.toJson();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public Password getPassword() {
        if (password == null) {
            password = Password.toPassword(jsonPassword);
        }
        return password;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }
}
