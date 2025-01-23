package org.abx.repository.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RepoConfig {
    public String user;
    public String name;
    public String engine;
    public String url;

    public String message;
    public Map<String, String> creds;

    public Set<String> diff;

    public String lastKnownStatus;

    public RepoConfig() {
        creds = new HashMap<>();
        diff = new HashSet<>();
        message ="push";
    }

    public RepoConfig updatedConfig;
}
