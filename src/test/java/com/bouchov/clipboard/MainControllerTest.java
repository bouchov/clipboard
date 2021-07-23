package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.AccountRepository;
import com.bouchov.clipboard.entities.Password;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;
import java.util.UUID;

import static com.bouchov.clipboard.SessionAttributes.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alexandre Y. Bouchov
 * Date: 21.07.2021
 * Time: 16:48
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MainControllerTest {
    private static final String PASSWORD = "pwd";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountRepository accountRepository;
    private Account testAccount;

    @BeforeEach
    public void setUp() {
        Account user = new Account(UniqSource.uniqueString("name"), Password.create(PASSWORD), new Date());
        testAccount = accountRepository.save(user);
    }

    @Test public void testLogin() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/")
                .param("name", testAccount.getName())
                .param("password", PASSWORD)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.account.id", Matchers.is(testAccount.getId().intValue())))
                .andExpect(jsonPath("$.device").exists())
                .andExpect(status().isOk());
    }

    @Test public void testLoginWrong() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/")
                .param("name", UniqSource.uniqueString("name"))
                .param("password", PASSWORD)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test public void testLoginDifferent() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/")
                .param("name", testAccount.getName())
                .param("password", PASSWORD)
                .sessionAttr(ACCOUNT, UniqSource.uniqueLong(testAccount.getId()))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test public void testRegister() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/register")
                .param("name", UniqSource.uniqueString("test"))
                .param("password", PASSWORD)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.account").exists())
                .andExpect(jsonPath("$.device").exists())
                .andExpect(status().isOk());
    }

    @Test public void testPingExpired() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/ping"))
                .andExpect(status().isNotFound());
    }

    @Test public void testPingSignedIn() throws Exception {
        UUID device = UniqSource.uuid();
        mvc.perform(MockMvcRequestBuilders.get("/ping")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .sessionAttr(DEVICE, device)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.account.id", Matchers.is(testAccount.getId().intValue())))
                .andExpect(jsonPath("$.device", Matchers.is(device.toString())))
                .andExpect(status().isOk());
    }

    @Test public void testPingAnonymous() throws Exception {
        UUID device = UniqSource.uuid();
        mvc.perform(MockMvcRequestBuilders.get("/ping")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .sessionAttr(DEVICE, device)
                .sessionAttr(TOKEN, UniqSource.uniqueString("token"))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.account").doesNotExist())
                .andExpect(jsonPath("$.device", Matchers.is(device.toString())))
                .andExpect(status().isOk());
    }

    @Test public void testSetClipboard() throws Exception {
        UUID device = UniqSource.uuid();
        mvc.perform(MockMvcRequestBuilders.post("/clipboard")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .sessionAttr(DEVICE, device)
                .content("[{\"type\":\"FILE\",\"data\":{\"name\":\"1.ppd\",\"size\":13552,\"type\":\"application/vnd.cups-ppd\",\"lastModified\":1265276168000},\"source\":\"" + device + "\"},{\"type\":\"FILE\",\"data\":{\"name\":\"2.ppd\",\"size\":14411,\"type\":\"application/vnd.cups-ppd\",\"lastModified\":1265276172000},\"source\":\"" + device + "\"}]")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test public void testSetClipboardAnonymous() throws Exception {
        UUID device = UniqSource.uuid();
        mvc.perform(MockMvcRequestBuilders.post("/clipboard")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .sessionAttr(DEVICE, device)
                .sessionAttr(TOKEN, UniqSource.uniqueString("token"))
                .content("[{\"type\":\"FILE\",\"data\":{\"name\":\"1.ppd\",\"size\":13552,\"type\":\"application/vnd.cups-ppd\",\"lastModified\":1265276168000},\"source\":\"" + device + "\"},{\"type\":\"FILE\",\"data\":{\"name\":\"2.ppd\",\"size\":14411,\"type\":\"application/vnd.cups-ppd\",\"lastModified\":1265276172000},\"source\":\"" + device + "\"}]")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test public void testGetClipboardEmpty() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/clipboard")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$", Matchers.hasSize(0)))
                .andExpect(status().isOk());
    }

    @Test public void testGetClipboard() throws Exception {
        testSetClipboard();

        mvc.perform(MockMvcRequestBuilders.get("/clipboard")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(status().isOk());
    }

    @Test public void testGetClipboardAnonymous() throws Exception {
        testSetClipboard();

        mvc.perform(MockMvcRequestBuilders.get("/clipboard")
                .sessionAttr(ACCOUNT, testAccount.getId())
                .sessionAttr(TOKEN, UniqSource.uniqueString("token"))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(status().isOk());
    }
}
