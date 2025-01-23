package org.abx.repository.engine;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.abx.repository.model.RepoConfig;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GitRepositoryEngine implements RepositoryEngine {
    private final static String Username = "username";
    private final static String Password = "password";
    private final static String Ssh = "ssh";
    private final static String Branch = "branch";
    private final static String Passphrase = "passphrase";
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
    public String update(RepoConfig config) {

        // Create the clone command
        File root = new File(dir + "/" + config.user + "/" + config.name);
        if (!root.exists()) {
            root.mkdirs();
            return clone(config);
        }
        File dotGit = new File(root, ".git");
        if (!dotGit.exists()) {
            if (!RepositoryEngine.deleteFolder(root)) {
                return "Unable to delete folder " + root.getAbsolutePath();
            }
            root.mkdirs();
            return clone(config);
        }
        String uri = getRemoteUri(root);
        if (!uri.equals(config.url)) {
            if (!RepositoryEngine.deleteFolder(root)) {
                return "Unable to delete folder " + root.getAbsolutePath();
            }
            root.mkdirs();
            return clone(config);
        }
        String configBranch = config.creds.get(Branch);

        String branch = getCurrentBranch(root);
        if (configBranch == null || configBranch.isEmpty() ||
                configBranch.equals(branch)) {
            return pull(root, config);
        } else {
            return setBranch(root, config);
        }
    }

    public String push(RepoConfig config) {
        throw new RuntimeException("push not implemented");
    }


    public String rollback(RepoConfig config) {
        throw new RuntimeException("rollback not implemented");
    }

    public String rollbackFile(RepoConfig config, String file) {
        throw new RuntimeException("rollback not implemented");
    }

    public String commit(RepoConfig config) {

        throw new RuntimeException("rollback not implemented");
    }

    public List<String> diff(RepoConfig config) {

        throw new RuntimeException("rollback not implemented");
    }

    private String pull(File repoDir, RepoConfig config) {
        try {
            Git git = Git.open(repoDir);
            PullCommand pullCommand = git.pull();
            setCreds(pullCommand, config);
            // Execute the clone command
            pullCommand.call();
            // Close the repository
            git.close();
            return WorkingSince + new Date() + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "Issue with Git: " + e.getMessage();
        }
    }


    private String setBranch(File repoDir, RepoConfig config) {
        try {
            Git git = Git.open(repoDir);
            CheckoutCommand checkoutCommand = git.checkout().
                    setName(config.creds.get(Branch));
            // Execute the clone command
            checkoutCommand.call();
            // Close the repository
            git.close();
            return pull(repoDir, config);
        } catch (Exception e) {
            e.printStackTrace();
            return "Issue with Git: " + e.getMessage();
        }
    }


    private String clone(RepoConfig config) {
        try {
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(config.url)
                    .setRemote("origin")
                    .setDirectory(new File(dir + "/" + config.user + "/" + config.name));
            setCreds(cloneCommand, config);
            // Execute the clone command
            Git git = cloneCommand.call();
            // Close the repository
            git.close();
            return WorkingSince + new Date() + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "Issue with Git: " + e.getMessage();
        }
    }

    private void setCreds(TransportCommand command, RepoConfig config) {
        Map<String, String> creds = config.creds;
        if (creds.containsKey(Username)) {
            CredentialsProvider credsProvider = new UsernamePasswordCredentialsProvider
                    (creds.get(Username), creds.get(Password));
            command.setCredentialsProvider(credsProvider);
        } else if (creds.containsKey(Ssh)) {
            String privateKey = creds.get(Ssh);
            byte[] privateKeyBytes = privateKey.getBytes(StandardCharsets.UTF_8);
            byte[] passphraseBytes;
            if (creds.containsKey(Passphrase)) {
                passphraseBytes = creds.get(Passphrase).getBytes(StandardCharsets.UTF_8);
            } else {
                passphraseBytes = null;
            }
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    // Additional configurations if needed
                    session.setConfig("StrictHostKeyChecking", "no");
                }

                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch jsch = super.createDefaultJSch(fs);
                    jsch.addIdentity("keyIdentifier", privateKeyBytes, null, passphraseBytes); // `null` for no passphrase or public key
                    return jsch;
                }
            };
            command.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport sshTransport) {
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        } else if (config.url.startsWith("git")) {
            command.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport sshTransport) {
                    sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
                        @Override
                        protected void configure(OpenSshConfig.Host hc, Session session) {
                            // Additional configurations if needed
                            session.setConfig("StrictHostKeyChecking", "no");
                        }
                    });
                }
            });
        }
    }

    public static String getRemoteUri(File repoDir) {
        try {// Open the repository
            Git git = Git.open(repoDir);
            Repository repository = git.getRepository();
            // Retrieve the remote configuration for the specified remote

            List<RemoteConfig> remoteConfigs = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
            for (RemoteConfig remoteConfig : remoteConfigs) {
                // Return the first URL associated with the remote
                if (!remoteConfig.getURIs().isEmpty()) {
                    return remoteConfig.getURIs().get(0).toString();
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getCurrentBranch(File repoDir) {
        try {// Open the repository located at the specified path
            Git git = Git.open(repoDir);
            Repository repository = git.getRepository();
            // Return the current branch
            return repository.getBranch();
        } catch (Exception e) {
            return "";
        }
    }

}
