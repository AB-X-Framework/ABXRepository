package org.abx.repository.model;

import java.util.HashMap;
import java.util.Map;

public class ConfigHolder {

    public Map<String, UserRepoConfig> configurations;

    public ConfigHolder() {
        configurations = new HashMap<>();
    }
}
