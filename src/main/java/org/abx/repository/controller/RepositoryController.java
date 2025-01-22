package org.abx.repository.controller;

import jakarta.servlet.ServletException;
import org.abx.repository.model.ConfigHolder;
import org.abx.repository.model.RepoConfig;
import org.json.JSONObject;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.userdetails.User;
import java.io.IOException;
import java.util.HashMap;

@RestController
@RequestMapping("/repository")
public class RepositoryController {

    ConfigHolder configHolder;

    public RepositoryController(){
        configHolder = new ConfigHolder();
    }
    @Secured("repository")
    @RequestMapping(value = "/update")
    public boolean update(@AuthenticationPrincipal User user,
                          @RequestParam  String engine,
                          @RequestParam  String name,
                          @RequestParam  String url,
                          @RequestParam  String creds) throws ServletException, IOException {
        RepoConfig repoConfig = new RepoConfig();
        repoConfig.name = name;
        repoConfig.url = url;
        repoConfig.engine = engine;
        repoConfig.creds = new HashMap<>();
        JSONObject jsonCreds = new JSONObject(creds);
        for (String key : jsonCreds.keySet()) {
            repoConfig.creds.put(key, jsonCreds.getString(key));
        }

    }

}
