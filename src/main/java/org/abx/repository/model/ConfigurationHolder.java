package org.abx.repository.model;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationHolder {

    public Map<String, UserRepoConfig> configurations;

    public ConfigurationHolder() {
        configurations = new HashMap<>();
    }
}
