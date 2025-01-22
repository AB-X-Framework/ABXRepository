package org.abx.repository.model;

public interface RepositoryEngine {

    /**
     * Update clone, change branch whatever
     * @param config
     * @throws Exception
     */
    public void update(RepoConfig config) throws Exception;

    public void delete(RepoConfig config) throws Exception;

    public void save(RepoConfig config, String path, byte[] data) throws Exception;

    public byte[] get(RepoConfig config, String path) throws Exception;

    public byte[] zip(RepoConfig config, String path) throws Exception;
}
