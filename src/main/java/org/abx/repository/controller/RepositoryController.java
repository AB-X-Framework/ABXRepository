package org.abx.repository.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.abx.repository.engine.RepositoryEngine;
import org.abx.repository.model.ConfigHolder;
import org.abx.repository.model.RepoConfig;
import org.abx.repository.model.RepoReq;
import org.abx.repository.model.UserRepoConfig;
import org.abx.util.StreamUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/repository")
public class RepositoryController {


    public static final String Initializing = "Initializing";

    protected final ConcurrentLinkedQueue<RepoReq> reqs;
    protected final Semaphore semaphore;
    @Value("${repository.dir}")
    private String dir;

    private final ConfigHolder configHolder;
    private final HashMap<Integer, String> files;
    private RepositoryProcessor repositoryProcessor;

    public RepositoryController() {
        configHolder = new ConfigHolder();
        reqs = new ConcurrentLinkedQueue<>();
        semaphore = new Semaphore(0);
        files = new HashMap<>();
    }


    @PostConstruct
    public void init() {
        new File(dir).mkdirs();
        repositoryProcessor = new RepositoryProcessor(dir, this);

        repositoryProcessor.start();
    }

    @Secured("repository")
    @RequestMapping(value = "/update")
    public boolean update(HttpServletRequest request,
                          @RequestParam String engine,
                          @RequestParam String name,
                          @RequestParam String url,
                          @RequestParam String creds) throws ServletException, IOException {
        String username = request.getUserPrincipal().getName();
        RepoConfig repoConfig = new RepoConfig();
        repoConfig.lastKnownStatus = Initializing;
        repoConfig.name = name;
        repoConfig.url = url;
        repoConfig.user = username;
        repoConfig.engine = engine;
        JSONObject jsonCreds = new JSONObject(creds);
        for (String key : jsonCreds.keySet()) {
            repoConfig.creds.put(key, jsonCreds.getString(key));
        }
        if (!configHolder.containsKey(username)) {
            configHolder.put(username, new UserRepoConfig());
        }
        UserRepoConfig config = configHolder.get(username);
        if (config.containsKey(name)) {
            RepoConfig rConfig = config.get(name);
            if (!rConfig.valid) {
                return false;
            }
            rConfig.updatedConfig = repoConfig;
            reqs.add(new RepoReq("replace", repoConfig));
        } else {
            config.put(name, repoConfig);
            reqs.add(new RepoReq("update", repoConfig));
        }
        semaphore.release();
        return true;
    }


    @Secured("repository")
    @RequestMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Map<String,String>> status(HttpServletRequest request) {
        UserRepoConfig userConfig = configHolder.get(request.getUserPrincipal().getName());
        Map<String, Map<String,String>> result = new HashMap<>();
        if (userConfig == null) {
            return result;
        }
        for (RepoConfig config : userConfig.values()) {
            result.put(config.name, Map.of("status",config.lastKnownStatus,
                    "branch",config.branch));
        }
        return result;
    }

    private JSONObject getData(File file, String path, boolean inner) {
        JSONObject jsonObject = new JSONObject();
        String name = file.getName();
        jsonObject.put("name", name);
        jsonObject.put("path", path);
        int id = path.hashCode();
        files.put(id, path);
        jsonObject.put("id", id);
        if (file.isFile()) {
            long size = Math.max(1, file.length() / 1024);
            jsonObject.put("size", size + " KB");
            jsonObject.put("type", "file");
            jsonObject.put("closed", true);
        } else if (file.isDirectory()) {
            jsonObject.put("type", "folder");
            jsonObject.put("size", "");
            if (inner) {
                JSONArray children = new JSONArray();
                jsonObject.put("children", children);

                File[] files = file.listFiles();
                Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && f2.isFile()) {
                        return -1;
                    } else if (f1.isFile() && f2.isDirectory()) {
                        return 1;
                    }
                    return f1.getName().compareTo(f2.getName());
                });
                for (int i = 0; i < files.length; ++i) {
                    children.put(getData(files[i], path + "/" + files[i].getName(), false));
                }
                if (files.length == 0) {
                    jsonObject.put("state", "closed");
                }
            } else {
                jsonObject.put("state", "closed");
            }
        }
        return jsonObject;
    }


    @Secured("repository")
    @GetMapping(path = "/details", produces = "application/json")
    public String details(HttpServletRequest req, @RequestParam(name = "id", required = false) String id) throws Exception {
        File workingFolder = new File(dir + "/" + req.getUserPrincipal().getName());
        if (id == null) {
            JSONArray data = getData(workingFolder, "", true).getJSONArray("children");
            return data.toString(1);
        } else {
            String path = files.get(Integer.parseInt(id));
            return getData(new File(workingFolder, path), path, true).getJSONArray("children").toString(0);
        }
    }

    @Secured("repository")
    @GetMapping("/data")
    public ResponseEntity<InputStreamResource> data(HttpServletRequest req, @RequestParam String path) throws Exception {
        File workingFile = new File(dir + "/" +
                req.getUserPrincipal().getName() + path);
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" +
                workingFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(workingFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(new FileInputStream(workingFile)));
    }

    @Secured("repository")
    @PostMapping("/upload")
    public boolean upload(HttpServletRequest req,
                          @RequestParam("path") String path,
                          @RequestParam("file") MultipartFile file) throws IOException {
        String repository = path.substring(1, path.indexOf('/', 1));
        RepoConfig repoConfig = configHolder.get(req.getUserPrincipal().
                getName()).get(repository);
        if (!repoConfig.valid) {
            return false;
        }
        repoConfig.lastKnownStatus = "Updating";
        File workingFile = new File(dir + "/" +
                req.getUserPrincipal().getName() + path);
        InputStream reqInputStream = file.getInputStream();
        FileOutputStream fileOutputStream = new FileOutputStream(workingFile);
        StreamUtils.copyStream(reqInputStream, fileOutputStream);
        fileOutputStream.close();
        reqInputStream.close();
        reqs.add(new RepoReq("diff", repoConfig));
        semaphore.release();
        return true;
    }

    /**
     * This requests last known diff, but actual diff gets trigger during load
     *
     * @param req        the auth request
     * @param repository the repository name
     * @return the last know diff
     * @throws Exception Not found
     */
    @GetMapping(path = "/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> diff(HttpServletRequest req,
                             @RequestParam("repository") String repository) throws Exception {
        return configHolder.get(req.getUserPrincipal().
                getName()).get(repository).diff;

    }

    @GetMapping(path = "/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean remove(HttpServletRequest req,
                          @RequestParam("repository") String repository) throws Exception {
        RepoConfig repoConfig = configHolder.get(req.getUserPrincipal().
                getName()).get(repository);
        repoConfig.lastKnownStatus = "Deleting";
        repoConfig.valid = false;
        reqs.add(new RepoReq("remove", repoConfig));
        semaphore.release();
        return true;
    }

    /**
     * Disposes file
     *
     * @param username
     * @param configname
     */
    protected void dispose(String username, String configname) {
        configHolder.get(username).remove(configname);
    }

    @GetMapping(path = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean reset(HttpServletRequest req,
                         @RequestParam("repository") String repository) throws Exception {
        RepoConfig repoConfig = configHolder.get(req.getUserPrincipal().
                getName()).remove(repository);
        repoConfig.lastKnownStatus = "Resetting";
        reqs.add(new RepoReq("reset", repoConfig));
        semaphore.release();
        return true;
    }
}
