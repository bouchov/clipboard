package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.ContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 18:45
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@RestController
@RequestMapping("/clipboard")
public class ClipboardController {
    private final ContentRepository contentRepository;
    private final HttpSession session;

    @Autowired
    public ClipboardController(ContentRepository contentRepository, HttpSession session) {
        this.contentRepository = contentRepository;
        this.session = session;
    }
}
