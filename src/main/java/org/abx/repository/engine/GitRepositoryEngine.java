package org.abx.repository.engine;

import org.abx.repository.model.RepoConfig;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitRepositoryEngine implements RepositoryEngine {
    private final static String Username = "username";
    private final static String Password = "password";
    private String dir;

    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * Update clone, change branch whatever
     *
     * @param config
     * @throws Exception
     */
    public void update(RepoConfig config) throws Exception {
        // Create the clone command
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(config.url)
                .setDirectory(new File(dir + "/" + config.user + "/" + config.name))
                .setCredentialsProvider(getCredentialsProvider(config));
        // Execute the clone command
        Git git = cloneCommand.call();
        // Close the repository
        git.close();
    }

    public void rollback(RepoConfig config) throws Exception{
        throw new RuntimeException("rollback not implemented");
    }

    public void commit(RepoConfig config) throws Exception{

        throw new RuntimeException("rollback not implemented");
    }

    public List<String> diff(RepoConfig config) throws Exception{

        throw new RuntimeException("rollback not implemented");
    }

    private CredentialsProvider getCredentialsProvider(RepoConfig config) throws Exception {
        Map<String, String> creds = config.creds;
        if (creds.containsKey(Username)) {
            return new UsernamePasswordCredentialsProvider(creds.get(Username), creds.get(Password));
        } else {
            throw new Exception("Invalid credentials");
        }
    }
}
