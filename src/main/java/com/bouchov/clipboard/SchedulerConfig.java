package com.bouchov.clipboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Alexandre Y. Bouchov
 * Date: 15.07.2021
 * Time: 12:42
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
@Configuration
public class SchedulerConfig {
    private final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    @Value("${thread.pool.size:5}")
    private int poolSize;

    @Bean
    public ThreadPoolTaskScheduler quizScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(poolSize);
        String namePrefix = "QuizScheduler-";
        threadPoolTaskScheduler.setThreadNamePrefix(namePrefix);
        log.debug("Starting scheduler {} with {} threads", namePrefix, poolSize);
        return threadPoolTaskScheduler;
    }
}
