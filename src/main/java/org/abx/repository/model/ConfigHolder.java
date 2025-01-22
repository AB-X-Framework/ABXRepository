package org.abx.repository.model;

import java.util.HashMap;
import java.util.Map;

public class ConfigHolder {

    public Map<String, UserRepoConfig> configurations;
    public Map<String, String> diff;

    public ConfigHolder() {
        configurations = new HashMap<>();
        diff = new HashMap<>();
    }
}
