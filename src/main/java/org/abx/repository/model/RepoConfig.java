package org.abx.repository.model;

import java.util.*;

public class RepoConfig {
    public String user;
    public String name;
    public String engine;
    public String url;

    public String message;
    public Map<String, String> creds;

    public List<String> diff;

    public String branch;
    public String lastKnownStatus;

    public boolean valid;

    public RepoConfig() {
        creds = new HashMap<>();
        diff = new ArrayList<>();
        message ="push";
        branch = "";
    }

    public RepoConfig updatedConfig;
}
