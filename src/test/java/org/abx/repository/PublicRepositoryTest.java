package org.abx.repository;

import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServicesClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(classes = ABXRepositoryEntry.class)
public class PublicRepositoryTest {
    private static ConfigurableApplicationContext context;

    @Autowired
    JWTUtils jwtUtils;


    @Autowired
    ServicesClient servicesClient;


    @Value("${jwt.private}")
    private String privateKey;

    @BeforeAll
    public static void setup() {
        context = SpringApplication.run(ABXRepositoryEntry.class);
    }

    @Test
    public void doBasicTest() throws Exception {
    }

    @AfterAll
    public static void teardown() {
        context.stop();
    }
}
