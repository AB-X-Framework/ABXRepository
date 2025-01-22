package org.abx.repository.engine;

import org.abx.repository.model.RepoConfig;

import java.util.List;

public interface RepositoryEngine {

    public void setDir(String dir);

    /**
     * Update clone, change branch whatever
     *
     */
    public String update(RepoConfig config);

    public String push(RepoConfig config);

    public String rollbackFile(RepoConfig config, String path);

    public String rollback(RepoConfig config) ;

    public String commit(RepoConfig config) ;

    public List<String> diff(RepoConfig config) t;
}
