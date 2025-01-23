package org.abx.repository.model;

public class RepoReq {
    public String req;
    public String file;
    public RepoConfig config;

    public RepoReq(String req, RepoConfig config) {
        this.req = req;
        this.config = config;
    }


    public RepoReq(String req, RepoConfig config, String file) {
        this.req = req;
        this.file = file;
        this.config = config;
    }
}
