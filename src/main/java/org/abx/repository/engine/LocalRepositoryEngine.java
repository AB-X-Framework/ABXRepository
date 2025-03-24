package org.abx.repository.engine;

import org.abx.repository.model.RepoConfig;

import java.io.File;
import java.util.*;

public class LocalRepositoryEngine implements RepositoryEngine {
    private final static String Public = "public";
    private final static String Username = "username";
    private final static String Password = "password";
    public final static String Ssh = "ssh";
    private final static String Passphrase = "passphrase";
    private String dir;

    @Override
    public void setDir(String dir) {
        this.dir = dir;
    }

    @Override
    public String pull(RepoConfig config) {
        return WorkingSince + new Date()+ ".";
    }

    /**
     * Update clone, change branch whatever
     *
     * @param config
     * @throws Exception
     */
    @Override
    public String reset(RepoConfig config) {
        File root = new File(dir + "/" + config.user + "/" + config.repositoryName);
        if (!root.exists()) {
            root.mkdirs();
        }

        return WorkingSince + new Date()+ ".";
    }

    @Override
    public String push(RepoConfig config, List<String> files, String pushMessage) {
        return WorkingSince + new Date()+ ".";
    }

    @Override
    public String rollbackFile(RepoConfig config, List<String> files) {
        return WorkingSince + new Date()+ ".";
    }

    @Override
    public String diff(RepoConfig config) {
        return WorkingSince + new Date() + ".";

    }

    @Override
    public boolean validate(RepoConfig repoConfig) {
        return true;
    }

}
