package org.abx.repository.controller;

import jakarta.servlet.ServletException;
import org.abx.repository.model.ConfigHolder;
import org.abx.repository.model.RepoConfig;
import org.abx.repository.model.RepoReq;
import org.json.JSONObject;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/repository")
public class RepositoryController {

    private ConfigHolder configHolder;
    protected ConcurrentLinkedQueue<RepoReq> reqs;
    protected Semaphore semaphore;
    public RepositoryController() {
        configHolder = new ConfigHolder();
        reqs = new ConcurrentLinkedQueue <>();
        semaphore = new Semaphore(0);
        new RepositoryProcessor(this).start();
    }

    @Secured("repository")
    @RequestMapping(value = "/update")
    public void update(@AuthenticationPrincipal User user,
                          @RequestParam String engine,
                          @RequestParam String name,
                          @RequestParam String url,
                          @RequestParam String creds) throws ServletException, IOException {
        RepoConfig repoConfig = new RepoConfig();
        repoConfig.name = name;
        repoConfig.url = url;
        repoConfig.engine = engine;
        JSONObject jsonCreds = new JSONObject(creds);
        for (String key : jsonCreds.keySet()) {
            repoConfig.creds.put(key, jsonCreds.getString(key));
        }
        reqs.add(new RepoReq("update", repoConfig));
    }

}
