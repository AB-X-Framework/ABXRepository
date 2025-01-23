package org.abx.repository;

import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServiceRequest;
import org.abx.services.ServiceResponse;
import org.abx.services.ServicesClient;
import org.json.JSONObject;
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

import static org.abx.repository.controller.RepositoryController.Initializing;
import static org.abx.repository.engine.RepositoryEngine.WorkingSince;

@SpringBootTest(classes = ABXRepositoryEntry.class)
public class CloneTest {
    @Autowired
    JWTUtils jwtUtils;

    private static ConfigurableApplicationContext context;
    @Autowired
    ServicesClient servicesClient;


    @Value("${jwt.private}")
    private String privateKey;

    @BeforeAll
    public static void setup() {
        context = SpringApplication.run(ABXRepositoryEntry.class);
    }

    @Test
    public void doTest() throws Exception {
        String token = JWTUtils.generateToken("dummy", privateKey, 60,
                List.of("repository"));
        ServiceRequest req = servicesClient.post("repository", "/repository/update");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("name", "repo");
        req.addPart("url", "git@github.com:luislara/simplerepo.git");
        req.addPart("creds", "{}");
        ServiceResponse resp = servicesClient.process(req);
        Assertions.assertEquals(200, resp.statusCode());

        req = servicesClient.get("repository", "/repository/status");
        req.jwt(token);
        resp = servicesClient.process(req);
        JSONObject r = resp.asJSONObject();
        System.out.println(r.toString());
        Assertions.assertEquals(Initializing, r.get("repo"));

        boolean working = false;
        String status = null;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/status");
            req.jwt(token);
            resp = servicesClient.process(req);
            r = resp.asJSONObject();
            System.out.println(r.toString());
            status= r.getString("repo");
            if (status.startsWith(WorkingSince)) {
                working = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(working, status);
    }

    @AfterAll
    public static void teardown() {
        context.stop();
    }
}
