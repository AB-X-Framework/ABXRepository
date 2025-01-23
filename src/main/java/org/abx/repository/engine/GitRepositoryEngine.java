package org.abx.repository.engine;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.abx.repository.model.RepoConfig;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GitRepositoryEngine implements RepositoryEngine {
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
        File root = new File(dir + "/" + config.user + "/" + config.name);
        try (Git git = Git.open(root)) {
            PullCommand pullCommand = git.pull();
            pullCommand.setRemote("origin").setRemoteBranchName(getCurrentBranch(git));
            setCreds(pullCommand, config);
            pullCommand.call();
            return WorkingSince + new Date();
        } catch (Exception e) {
            return "Error with Git: " + e.getMessage();
        }
    }

    /**
     * Update clone, change branch whatever
     *
     * @param config
     * @throws Exception
     */
    @Override
    public String reset(RepoConfig config) {
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
        Git git = null;
        try {
            git = Git.open(root);
            String uri = getRemoteUri(git);
            if (!uri.equals(config.url)) {
                git.close();
                if (!RepositoryEngine.deleteFolder(root)) {
                    return "Unable to delete folder " + root.getAbsolutePath();
                }
                root.mkdirs();
                return clone(config);
            }
            if (config.branch.isEmpty()) {
                config.branch = getDefaultBranch(git.getRepository());
            }
            if (config.branch.equals(getCurrentBranch(git))) {
                pull(git, config);
            } else {
                setBranch(git, config);
            }
            git.close();
            return WorkingSince + new Date();
        } catch (Exception e) {
            if (git != null) {
                git.close();
            }
            if (!RepositoryEngine.deleteFolder(root)) {
                return "Unable to delete folder " + root.getAbsolutePath();
            }
            root.mkdirs();
            return clone(config);
        }

    }

    @Override
    public String push(RepoConfig config, List<String> files, String pushMessage) {
        File root = new File(dir + "/" + config.user + "/" + config.name);
        try (Git git = Git.open(root)) {
            // Checkout the file to revert it to its state in HEAD
            AddCommand addCommand = git.add();
            for (String fileName : files) {
                addCommand.addFilepattern(fileName);
            }
            addCommand.call();
            PushCommand pushCommand = git.push();
            setCreds(pushCommand, config);
            pushCommand.call();
        } catch (Exception e) {
            return "Error with Git: " + e.getMessage();
        }
        // Expecting git closed
        return diff(config);
    }

    @Override
    public String rollbackFile(RepoConfig config, List<String> files) {
        File root = new File(dir + "/" + config.user + "/" + config.name);
        try (Git git = Git.open(root)) {
            // Checkout the file to revert it to its state in HEAD
            CheckoutCommand checkoutCommand = git.checkout();
            for (String fileName : files) {
                checkoutCommand.addPath(fileName);
            }
            checkoutCommand.call();
        } catch (Exception e) {
            return "Error with Git: " + e.getMessage();
        }
        // Expecting git closed
        return diff(config);
    }

    @Override
    public String diff(RepoConfig config) {
        File root = new File(dir + "/" + config.user + "/" + config.name);
        try (Git git = Git.open(root)) {
            // Get the DiffCommand instance to compute differences
            DiffCommand diffCommand = git.diff();
            // Get all the differences between the working directory and the index (staging area)
            List<DiffEntry> diffs = diffCommand.call();
            // Get the list of file paths from the diffs
            List<String> filePaths = new ArrayList<>();
            // Loop through the DiffEntry list and extract the file paths
            for (DiffEntry diffEntry : diffs) {
                // You can use diffEntry.getNewPath() to get the path of the file in the current version
                filePaths.add(diffEntry.getNewPath());
            }
            config.diff = filePaths;
            return WorkingSince + new Date() + ".";
        } catch (Exception e) {
            return IssueWithGit + e.getMessage();
        }
    }

    private void pull(Git git, RepoConfig config) throws Exception {
        String branch = git.getRepository().getBranch();
        PullCommand pullCommand = git.pull();
        pullCommand.setRemote("origin").setRemoteBranchName(branch);
        setCreds(pullCommand, config);
        // Execute the clone command
        pullCommand.call();
    }

    private void setBranch(Git git, RepoConfig config) throws Exception {
        Ref ref = git.getRepository().findRef("refs/heads/" + config.branch);
        boolean create = ref == null;
        CheckoutCommand checkoutCommand = git.checkout().setName(config.branch);
        if (create) {
            checkoutCommand.setCreateBranch(true).setUpstreamMode
                            (CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint("origin/" + config.branch);
        }
        // Execute the clone command
        checkoutCommand.call();

    }

    private String clone(RepoConfig config) {
        try {
            CloneCommand cloneCommand = Git.cloneRepository().setURI(config.url).setRemote("origin").setDirectory(new File(dir + "/" + config.user + "/" + config.name));
            setCreds(cloneCommand, config);
            if (!config.branch.isEmpty()) {
                cloneCommand.setBranch(config.branch);
            }
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
            CredentialsProvider credsProvider = new UsernamePasswordCredentialsProvider(creds.get(Username), creds.get(Password));
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
                    JSch jsch = new JSch();
                    jsch.addIdentity("keyIdentifier", privateKeyBytes, null, passphraseBytes); // `null` for no passphrase or public key

                   return jsch;
                }
            };
            command.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport sshTransport) {
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        }
    }

    public static String getRemoteUri(Git git) throws Exception {
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
    }

    public static String getCurrentBranch(Git git) throws Exception {
        Repository repository = git.getRepository();
        // Return the current branch
        return repository.getBranch();
    }


    public static String getDefaultBranch(Repository repository) throws Exception {
        List<RemoteConfig> remotes = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
        for (RemoteConfig remote : remotes) {
            if ("origin".equals(remote.getName())) { // Check if it's the 'origin' remote
                List<URIish> uris = remote.getURIs();
                if (!uris.isEmpty()) {
                    // Use the first URI (assumes the repository has one origin URL)
                    String remoteUrl = uris.get(0).toString();
                    // Use lsRemoteRepository to fetch references
                    Collection<Ref> refs = Git.lsRemoteRepository()
                            .setRemote(remoteUrl)
                            .setHeads(false)
                            .setTags(false)
                            .call();

                    // Find the symbolic HEAD reference
                    for (Ref ref : refs) {
                        if ("HEAD".equals(ref.getName()) && ref.isSymbolic()) {
                            // Parse the default branch from the symbolic reference
                            String target = ref.getTarget().getName();
                            return target.substring("refs/heads/".length()); // Trim "refs/heads/" prefix
                        }
                    }
                }
            }
        }
        return ""; // Default branch not found
    }
}
