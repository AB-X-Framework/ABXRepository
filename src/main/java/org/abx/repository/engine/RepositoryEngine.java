package org.abx.repository.engine;

import org.abx.repository.model.RepoConfig;

import java.util.List;

public interface RepositoryEngine {

    public void setDir(String dir);
    /**
     * Update clone, change branch whatever
     * @param config
     * @throws Exception
     */
    public void update(RepoConfig config) throws Exception;

    public void rollback(RepoConfig config) throws Exception;

    public void commit(RepoConfig config) throws Exception;

    public List<String> diff(RepoConfig config)throws Exception;
}
