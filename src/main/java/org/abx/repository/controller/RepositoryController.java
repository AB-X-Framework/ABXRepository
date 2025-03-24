package org.abx.repository.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/repository")
public class RepositoryController {


    public static final String Initializing = "Initializing";

    protected final ConcurrentLinkedQueue<RepoReq> reqs;
    protected final Semaphore semaphore;
    @Value("${repository.dir}")
    private String dir;

    private final ConfigHolder configHolder;
    private final HashMap<Integer, RepositoryFile> files;
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

    @Secured("Repository")
    @RequestMapping(value = "/update/{repositoryName}")
    public boolean update(HttpServletRequest request,
                          @PathVariable String repositoryName,
                          @RequestParam String engine,
                          @RequestParam String url,
                          @RequestParam String branch,
                          @RequestParam String creds) throws ServletException, IOException {
        String username = request.getUserPrincipal().getName();
        RepoConfig repoConfig = new RepoConfig();
        repoConfig.lastKnownStatus = Initializing;
        repoConfig.repositoryName = repositoryName;
        repoConfig.url = url;
        repoConfig.user = username;
        repoConfig.branch = branch;
        repoConfig.engine = engine;
        JSONObject jsonCreds = new JSONObject(creds);
        for (String key : jsonCreds.keySet()) {
            repoConfig.creds.put(key, jsonCreds.getString(key));
        }
        if (!configHolder.containsKey(username)) {
            configHolder.put(username, new UserRepoConfig());
        }
        UserRepoConfig config = configHolder.get(username);
        if (config.containsKey(repositoryName)) {
            RepoConfig rConfig = config.get(repositoryName);
            if (!rConfig.valid) {
                return false;
            }
            rConfig.lastKnownStatus = Initializing;
            rConfig.updatedConfig = repoConfig;
            reqs.add(new RepoReq("replace", rConfig));
        } else {
            config.put(repositoryName, repoConfig);
            reqs.add(new RepoReq("reset", repoConfig));
        }
        semaphore.release();
        return true;
    }


    @Secured("Repository")
    @RequestMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Map<String, String>> status(HttpServletRequest request) {
        UserRepoConfig userConfig = configHolder.get(request.getUserPrincipal().getName());
        Map<String, Map<String, String>> result = new HashMap<>();
        if (userConfig == null) {
            return result;
        }
        for (RepoConfig config : userConfig.values()) {
            result.put(config.repositoryName, Map.of("status", config.lastKnownStatus,
                    "branch", config.branch));
        }
        return result;
    }

    private JSONObject getData(String username, String repository, File file, String path, boolean inner) {
        JSONObject jsonObject = new JSONObject();
        String name = file.getName();
        jsonObject.put("name", name);
        jsonObject.put("path", path);
        int id = (username + path).hashCode();
        files.put(id, new RepositoryFile(repository, path));
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
                    children.put(getData(username, repository, files[i], path + "/" + files[i].getName(), false));
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


    @Secured("Repository")
    @GetMapping(path = "/details", produces = "application/json")
    public String details(HttpServletRequest req, @RequestParam(name = "id", required = false) String id) throws Exception {
        String username = req.getUserPrincipal().getName();
        File workingFolder = new File(dir + "/" + username);
        if (id == null) {
            JSONArray data = new JSONArray();
            if (!configHolder.containsKey(username)) {
                return data.toString();
            }
            for (String repo : configHolder.get(username).keySet()) {
                data.put(getData(username, repo, new File(workingFolder, repo), "/" + repo, false));

            }
            return data.toString(1);
        } else {
            RepositoryFile repoFile = files.get(Integer.parseInt(id));
            //Path not valid
            if (!configHolder.get(username).containsKey(repoFile.repositoryName)) {
                return "[]";
            }
            String path = repoFile.path;
            return getData(username, repoFile.repositoryName, new File(workingFolder, path), path, true).getJSONArray("children").toString(0);
        }
    }

    @Secured("Repository")
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

    @Secured("Repository")
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
     *
     * @param req        the auth request
     * @param repositoryName the repository name
     * @return the last know diff
     */
    @Secured("Repository")
    @GetMapping(path = "/diff/{repositoryName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> diff(HttpServletRequest req,
                             @PathVariable String repositoryName) {
        return configHolder.get(req.getUserPrincipal().
                getName()).get(repositoryName).diff;
    }

    @Secured("Repository")
    @GetMapping(path = "/remove/{repositoryName}")
    public boolean remove(HttpServletRequest req,
                          @PathVariable String repositoryName) {
        RepoConfig repoConfig = configHolder.get(req.getUserPrincipal().
                getName()).get(repositoryName);
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
     * @param repositoryName
     */
    protected void dispose(String username, String repositoryName) {
        configHolder.get(username).remove(repositoryName);
    }

    @Secured("Repository")
    @RequestMapping(path = "/rollback/{repositoryName}")
    public boolean rollback(HttpServletRequest req,
                            @PathVariable String repositoryName,
                            @RequestParam String files) throws Exception {
        RepoConfig repoConfig = configHolder.get(req.getUserPrincipal().
                getName()).get(repositoryName);
        repoConfig.lastKnownStatus = "Rolling back";
        reqs.add(new RepoReq("rollback", repoConfig, files));
        semaphore.release();
        return true;
    }

    @Secured("Repository")
    @RequestMapping(path = "/push/{repositoryName}")
    public boolean push(HttpServletRequest req,
                        @PathVariable String repositoryName,
                        @RequestParam String pushMessage,
                        @RequestParam String files) throws Exception {
        RepoConfig repoConfig = configHolder.get(req.getUserPrincipal().
                getName()).get(repositoryName);
        repoConfig.lastKnownStatus = "Pushing";
        RepoReq repoReq = new RepoReq("push", repoConfig, files);
        repoReq.pushMessage = pushMessage;
        reqs.add(repoReq);
        semaphore.release();
        return true;
    }


    @Secured("Repository")
    @RequestMapping("/zip")
    public ResponseEntity<StreamingResponseBody> zip(HttpServletRequest req,
                                                                   @RequestParam String path) {
        String username = req.getUserPrincipal().getName();
        String repository = path.substring(1, path.indexOf('/', 1));
        RepoConfig repoConfig = configHolder.get(username).get(repository);
        if (!repoConfig.valid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(outputStream -> {
                        outputStream.write("Invalid repository".getBytes());
                    });
        }
        File folder = new File(dir+"/"+username+"/"+path);
        if (!folder.exists()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(outputStream -> {
                        outputStream.write("Invalid folder path!".getBytes());
                    });
        }
        StreamingResponseBody responseBody = outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                // Write folder entries to the ZipOutputStream dynamically
                addFolderToZip(zos, folder, "");
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+
                folder.getName()+".zip\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }

    private void addFolderToZip(ZipOutputStream zos, File folder, String basePath) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            return; // Empty folder or I/O error
        }
        for (File file : files) {
            String entryName = basePath + "/" + file.getName();
            if (file.isDirectory()) {
                // Add directory entry
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();
                // Recursively add subfolder contents
                addFolderToZip(zos, file, entryName);
            } else {
                // Add file entry
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    @Secured("Repository")
    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean validateCreds(
            @RequestParam String url, @RequestParam String branch,
            @RequestParam String engine,   @RequestParam String creds) {
        RepoConfig repoConfig = new RepoConfig();
        repoConfig.lastKnownStatus = Initializing;
        repoConfig.url = url;
        repoConfig.branch = branch;
        repoConfig.engine = engine;
        JSONObject jsonCreds = new JSONObject(creds);
        for (String key : jsonCreds.keySet()) {
            repoConfig.creds.put(key, jsonCreds.getString(key));
        }
        return repositoryProcessor.validate(repoConfig);
    }
}
