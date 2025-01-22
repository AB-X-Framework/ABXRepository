package org.abx.repository.model;

public class RepoReq {
    public String req;
    public RepoConfig config;

    public RepoReq(String req, RepoConfig config) {
        this.req = req;
        this.config = config;
    }
}
