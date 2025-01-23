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
        String repositoryName = "repo";
        String token = JWTUtils.generateToken("dummy", privateKey, 60,
                List.of("repository"));

        /*ServiceRequest req = servicesClient.post("repository", "/repository/remove");
        req.jwt(token);
        req.addPart("repository", repositoryName);
        boolean working = false;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/status");
            req.jwt(token);
            ServiceResponse resp = servicesClient.process(req);
            JSONObject r = resp.asJSONObject();
            System.out.println(r.toString());
            if (r.isEmpty()) {
                working = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(working);*/

       ServiceRequest  req = servicesClient.post("repository", "/repository/update");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("name", repositoryName);
        req.addPart("url", "https://github.com/luislara/simplerepo.git");
        req.addPart("creds", "{\"branch\":\"main\"}");
        ServiceResponse resp = servicesClient.process(req);
        Assertions.assertEquals(200, resp.statusCode());

        req = servicesClient.get("repository", "/repository/status");
        req.jwt(token);
        resp = servicesClient.process(req);
        JSONObject r = resp.asJSONObject();
        System.out.println(r.toString());
        Assertions.assertEquals(Initializing, r.getJSONObject("repo").get("status"));

        boolean working = false;
        String status = null;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/status");
            req.jwt(token);
            resp = servicesClient.process(req);
            r = resp.asJSONObject();
            System.out.println(r.toString());
            status = r.getJSONObject("repo").getString("status");
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
        JSONArray jsonArray = resp.asJSONArray();
        int id = idWithName(jsonArray, "repo");

        req = servicesClient.get("repository", "/repository/details?id=" + id);
        req.jwt(token);
        resp = servicesClient.process(req);
        System.out.println(resp.asJSONArray());


        String path ="/"+ repositoryName + "/README.md";
        req = servicesClient.get("repository", "/repository/data?path=" +
                path);
        req.jwt(token);
        resp = servicesClient.process(req);
        Assertions.assertEquals("simplerepo", resp.asString().trim());

        req = servicesClient.post("repository", "/repository/update");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("name", repositoryName);
        req.addPart("url", "https://github.com/luislara/simplerepo.git");
        req.addPart("creds", "{\"branch\":\"super\"}");
        req.jwt(token);
        resp = servicesClient.process(req);

        boolean found = false;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/data?path=" +
                    path);
            req.jwt(token);
            resp = servicesClient.process(req);
            if ("superbranch".equals(resp.asString().trim())) {
                found = true;
                break;
            }

            req = servicesClient.get("repository", "/repository/status");
            req.jwt(token);
            resp = servicesClient.process(req);
            r = resp.asJSONObject();
            System.out.println(r.toString());
            Thread.sleep(1000);
        }
        Assertions.assertTrue(found);

        req = servicesClient.post("repository", "/repository/upload");
        req.jwt(token);
        req.addPart("file", "newData".getBytes(), "data.txt");
        req.addPart("path", path);
        resp = servicesClient.process(req);
        Assertions.assertTrue(resp.asBoolean());


        req = servicesClient.get("repository", "/repository/diff?repository=repo");
        req.jwt(token);
        resp = servicesClient.process(req);
        System.out.println(resp.asJSONArray());

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
