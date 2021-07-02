package com.bouchov.clipboard;

import com.bouchov.clipboard.entities.Account;
import com.bouchov.clipboard.entities.AccountRepository;
import com.bouchov.clipboard.entities.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;

import javax.transaction.Transactional;

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
            admin = userRepository.save(new Account("admin", Password.create("Admin")));
            userRepository.save(admin);
            logger.info("service successfully initialized");
        };
    }

    @Bean
    public ServletListenerRegistrationBean<SessionListener> sessionListenerWithMetrics() {
        ServletListenerRegistrationBean<SessionListener> listenerRegBean =
                new ServletListenerRegistrationBean<>();
        listenerRegBean.setListener(new SessionListener());
        return listenerRegBean;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClipboardApplication.class, args);
    }

}
