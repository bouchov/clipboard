package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.AccountRepository;
import com.bouchov.clipboard.entities.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.transaction.Transactional;
import java.util.Date;

@SpringBootApplication
public class ClipboardApplication {
    private final Logger logger = LoggerFactory.getLogger(ClipboardApplication.class);

    @Bean
    @Transactional
    CommandLineRunner init(AccountRepository userRepository) {
        return (evt) -> {
            logger.info("initialize server");
            Account admin = userRepository.findByName("admin").orElse(null);
            if (admin != null) {
                logger.info("server already initialized");
                return;
            }
            admin = userRepository.save(new Account("admin", Password.create("Admin"), new Date()));
            userRepository.save(admin);
            logger.info("service successfully initialized");
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ClipboardApplication.class, args);
    }

}
