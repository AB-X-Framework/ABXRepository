package org.abx.repository;

import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServiceRequest;
import org.abx.services.ServiceResponse;
import org.abx.services.ServicesClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

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
        String repoName = "repo";
        String token = JWTUtils.generateToken("dummy", privateKey, 60,
                List.of("Repository"));


        ServiceRequest req = servicesClient.post("repository", "/repository/validate");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("url", "https://github.com/AB-X-Framework/git-simplerepo.git");
        req.addPart("creds", "{}");
        req.addPart("branch", "super");
        ServiceResponse resp = servicesClient.process(req);
        System.out.println(resp.asString());
        Assertions.assertEquals(200, resp.statusCode());
        Assertions.assertTrue(resp.asBoolean());


        req = servicesClient.post("repository", "/repository/validate");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("url", "https://github.com/AB-X-Framework/git-simplerepo.git");
        req.addPart("creds", "{}");
        req.addPart("branch", "super2");
        resp = servicesClient.process(req);
        Assertions.assertEquals(200, resp.statusCode());
        Assertions.assertFalse(resp.asBoolean());

        req = servicesClient.post("repository", "/repository/validate");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("url", "https://github.com/AB-X-Framework/git-simplerepo2.git");
        req.addPart("creds", "{}");
        req.addPart("branch", "super2");
        resp = servicesClient.process(req);
        Assertions.assertEquals(200, resp.statusCode());
        Assertions.assertFalse(resp.asBoolean());


    }

    @AfterAll
    public static void teardown() {
        context.stop();
    }
}
