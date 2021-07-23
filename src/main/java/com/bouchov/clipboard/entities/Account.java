package com.bouchov.clipboard.entities;

import com.fasterxml.jackson.core.JsonProcessingException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Date;

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
    @Column
    private Date registration;
    @Column
    private Date lastLogin;

    public Account() {
    }

    public Account(String name, Password password, Date registration) {
        this.name = name;
        this.password = password;
        this.registration = registration;
        this.lastLogin = registration;
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

    public Date getRegistration() {
        return registration;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "[Account super=" + super.toString() +
                ", name=" + (name == null ? null : '\'' + name + '\'') +
                ", password=" + password +
                ", registration=" + registration +
                ", lastLogin=" + lastLogin +
                ']';
    }
}
