package org.abx.repository.model;

import org.json.JSONArray;

import java.util.List;

public class RepoReq {
    public String req;
    public String pushMessage;
    public List<String> files;
    public RepoConfig config;

    public RepoReq(String req, RepoConfig config) {
        this.req = req;
        this.config = config;
    }


    public RepoReq(String req, RepoConfig config, String file) {
        this.req = req;
        this.files = (List) new JSONArray(file).toList();
        this.config = config;
    }
}
