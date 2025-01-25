package org.abx.repository;

import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServiceRequest;
import org.abx.services.ServiceResponse;
import org.abx.services.ServicesClient;
import org.abx.util.StreamUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.abx.repository.engine.GitRepositoryEngine.Ssh;
import static org.abx.repository.engine.RepositoryEngine.WorkingSince;

@SpringBootTest(classes = ABXRepositoryEntry.class)
@Disabled
public class ZipTest {
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
        String repositoryName = "ziprepo";
        String token = JWTUtils.generateToken("dummy", privateKey, 60,
                List.of("repository"));

        ServiceRequest req = servicesClient.post("repository", "/repository/update");
        req.jwt(token);
        req.addPart("engine", "git");
        req.addPart("name", repositoryName);
        req.addPart("url", "git@github.com:luislara/editRepo.git");
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
            status = jsonObject.getJSONObject(repositoryName).getString("status");
            if (status.startsWith(WorkingSince)) {
                working = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assertions.assertTrue(working, status);


        req = servicesClient.post("repository", "/repository/zip");
        req.jwt(token);
        req.addPart("path", "/" + repositoryName + "/zip");
        resp = servicesClient.process(req);
        extractZipToFolder(resp.asStream(), "zipFolder");
        Assertions.assertEquals("top", StreamUtils.readStream(
                new FileInputStream("zipFolder/top.txt")));
        Assertions.assertEquals("inner", StreamUtils.readStream(
                new FileInputStream("zipFolder/inner/inner.txt")));
    }

    public static void extractZipToFolder(InputStream zipInputStream, String targetFolderPath)
            throws IOException {
        File targetFolder = new File(targetFolderPath);

        // Ensure the target directory exists
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            throw new IOException("Failed to create target directory: " + targetFolderPath);
        }

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetFolder, entry.getName());

                if (entry.isDirectory()) {
                    // Create directories if the entry is a directory
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + outFile.getAbsolutePath());
                    }
                } else {
                    // Ensure parent directories exist
                    File parentDir = outFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                    }

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }

    @AfterAll
    public static void teardown() {
        context.stop();
    }
}
