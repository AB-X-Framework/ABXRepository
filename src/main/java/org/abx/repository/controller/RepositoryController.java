package org.abx.repository.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.abx.repository.model.ConfigHolder;
import org.abx.repository.model.RepoConfig;
import org.abx.repository.model.RepoReq;
import org.abx.repository.model.UserRepoConfig;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.userdetails.User;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/repository")
public class RepositoryController {

    @Value("${repository.dir}")
    private String dir;
    private ConfigHolder configHolder;
    protected ConcurrentLinkedQueue<RepoReq> reqs;
    protected Semaphore semaphore;

    public RepositoryController() {
        configHolder = new ConfigHolder();
        reqs = new ConcurrentLinkedQueue<>();
        semaphore = new Semaphore(0);
        new RepositoryProcessor(this).start();
    }


    @PostConstruct
    public void init() {
        new File(dir).mkdirs();
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
        repoConfig.lastKnownStatus = "Initializing";
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
        if (config.containsKey(repoConfig.name)) {
            config.get(username).updatedConfig = repoConfig;
            reqs.add(new RepoReq("replace", repoConfig));
        } else {
            config.put(username, repoConfig);
            reqs.add(new RepoReq("update", repoConfig));
        }
        semaphore.release();
        return true;
    }



    @Secured("repository")
    @RequestMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> status(HttpServletRequest request){
        Map<String,String> result = new HashMap<>();
        for (RepoConfig config : configHolder.get(request.getUserPrincipal().getName()).values()){
            result.put(config.name, config.lastKnownStatus);
        }
        return result;
    }

}
