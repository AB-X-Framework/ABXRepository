package org.abx.repository;

import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServiceRequest;
import org.abx.services.ServiceResponse;
import org.abx.services.ServicesClient;
import org.abx.util.StreamUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.FileInputStream;
import java.util.List;

import static org.abx.repository.controller.RepositoryController.Initializing;
import static org.abx.repository.engine.GitRepositoryEngine.Ssh;
import static org.abx.repository.engine.RepositoryEngine.WorkingSince;

@SpringBootTest(classes = ABXRepositoryEntry.class)
@Disabled
public class PushTest {
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
        String repositoryName = "repo2";
        String token = JWTUtils.generateToken("dummy", privateKey, 60,
                List.of("repository"));

        ServiceRequest req = servicesClient.post("repository", "/repository/update");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("name", repositoryName);
        req.addPart("url", "git@github.com:AB-X-Framework/editRepo.git");
        String key = StreamUtils.readStream(new FileInputStream("C:/Users/l3cla/.ssh/id_rsa"));
        req.addPart("creds", new JSONObject().put(Ssh, key).toString());
        req.addPart("branch", "");
        ServiceResponse resp = servicesClient.process(req);
        Assertions.assertEquals(200, resp.statusCode());

        boolean working = false;
        String status = null;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/status");
            req.jwt(token);
            resp = servicesClient.process(req);
            JSONObject jsonObject = resp.asJSONObject();
            System.out.println(jsonObject.toString());
            status = jsonObject.getJSONObject("repo").getString("status");
            if (status.startsWith(WorkingSince)) {
                working = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(working, status);

        String filename = "README.md";
        String path = "/"+repositoryName + "/" + filename;
        req = servicesClient.post("repository", "/repository/upload");
        req.jwt(token);
        String newData = "HELLO " + Math.random();
        req.addPart("file", newData.getBytes(), "data.txt");
        req.addPart("path", path);
        resp = servicesClient.process(req);
        Assertions.assertTrue(resp.asBoolean());
        boolean found = false;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/diff?repository=repo");
            req.jwt(token);
            resp = servicesClient.process(req);
            JSONArray jsonArray = resp.asJSONArray();
            if (jsonArray.length() > 0) {
                String diff = jsonArray.getString(0);
                if (diff.equals(filename)) {
                    found = true;
                    break;
                }
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(found);

        req = servicesClient.post("repository", "/repository/push");
        req.jwt(token);
        req.addPart("repository", repositoryName);
        req.addPart("pushMessage", "This is a push message");
        req.addPart("files", new JSONArray().put(filename).toString());
        resp = servicesClient.process(req);
        System.out.println(resp.asString());
        Assertions.assertTrue(resp.asBoolean());
        boolean zero = false;
        for (int i = 0; i < 10; ++i) {
            req = servicesClient.get("repository", "/repository/diff?repository=repo");
            req.jwt(token);
            resp = servicesClient.process(req);
            JSONArray jsonArray = resp.asJSONArray();
            if (jsonArray.isEmpty()) {
                zero = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(zero);
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
