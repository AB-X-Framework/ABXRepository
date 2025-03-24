package org.abx.repository;

import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServiceRequest;
import org.abx.services.ServiceResponse;
import org.abx.services.ServicesClient;
import org.json.JSONArray;
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
public class LocalRepoTest {
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
        String repositoryName = "repo";
        String token = JWTUtils.generateToken("Localuser", privateKey, 60,
                List.of("Repository"));

        ServiceRequest req = servicesClient.get("repository", "/repository/status");
        req.jwt(token);
        ServiceResponse resp = servicesClient.process(req);
        JSONObject jsonObject = resp.asJSONObject();
        System.out.println(jsonObject.toString());
        Assertions.assertTrue(jsonObject.isEmpty());

        req = servicesClient.get("repository", "/repository/details");
        req.jwt(token);
        resp = servicesClient.process(req);
        JSONArray jsonArray = resp.asJSONArray();
        Assertions.assertTrue(jsonArray.isEmpty());

        req = servicesClient.post("repository", "/repository/update/"+repositoryName);
        req.jwt(token);
        req.addPart("engine", "local");
        req.addPart("url", "");
        req.addPart("creds", "{}");
        req.addPart("branch", "");
        resp = servicesClient.process(req);
        Assertions.assertEquals(200, resp.statusCode());

        req = servicesClient.get("repository", "/repository/status");
        req.jwt(token);
        resp = servicesClient.process(req);
        jsonObject = resp.asJSONObject();
        System.out.println(jsonObject.toString());
        String statusText = jsonObject.getJSONObject("repo").getString("status");
        Assertions.assertTrue(statusText.contains(Initializing)
                || statusText.contains(WorkingSince) );

        boolean working = false;
        String status = null;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/status");
            req.jwt(token);
            resp = servicesClient.process(req);
            jsonObject = resp.asJSONObject();
            System.out.println(jsonObject.toString());
            status = jsonObject.getJSONObject("repo").getString("status");
            if (status.startsWith(WorkingSince)) {
                working = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(working, status);

        req = servicesClient.get("repository", "/repository/details");
        req.jwt(token);
        resp = servicesClient.process(req);
        jsonArray = resp.asJSONArray();
        int id = idWithName(jsonArray, "repo");

        req = servicesClient.get("repository", "/repository/details?id=" + id);
        req.jwt(token);
        resp = servicesClient.process(req);
        System.out.println(resp.asJSONArray());

        String filename = "README.md";
        String path = "/" + repositoryName + "/" + filename;

        req = servicesClient.post("repository", "/repository/upload");
        req.jwt(token);
        req.addPart("file", "newData".getBytes(), "data.txt");
        req.addPart("path", path);
        resp = servicesClient.process(req);
        Assertions.assertTrue(resp.asBoolean());


        boolean found = false;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/data?path=" +
                    path);
            req.jwt(token);
            resp = servicesClient.process(req);
            if ("newData".equals(resp.asString().trim())) {
                found = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(found);
    }

    private int idWithName(JSONArray jsonArray, String name) {
        for (int i = 0; i < jsonArray.length(); ++i) {
            JSONObject obj = jsonArray.getJSONObject(i);
            if (obj.getString("name").equals(name)) {
                return obj.getInt("id");
            }
        }
        return -1;
    }

    @AfterAll
    public static void teardown() {
        context.stop();
    }
}
