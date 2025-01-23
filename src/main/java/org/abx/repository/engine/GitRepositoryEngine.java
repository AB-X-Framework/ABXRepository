package org.abx.repository.engine;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.abx.repository.model.RepoConfig;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GitRepositoryEngine implements RepositoryEngine {
    private final static String Username = "username";
    private final static String Password = "password";
    private final static String Ssh = "ssh";
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
    public String update(RepoConfig config)  {
        try {
            // Create the clone command
            File root = new File(dir + "/" + config.user + "/" + config.name);
            root.mkdirs();
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(config.url)
                    .setDirectory(new File(dir + "/" + config.user + "/" + config.name));
            setCreds(cloneCommand, config);
            // Execute the clone command
            Git git = cloneCommand.call();
            // Close the repository
            git.close();
            return WorkingSince+new Date()+".";
        } catch (Exception e) {
            return "Issue with Git: " + e.getMessage();
        }
    }

    public String push(RepoConfig config)  {
        throw new RuntimeException("push not implemented");
    }


    public String rollback(RepoConfig config)  {
        throw new RuntimeException("rollback not implemented");
    }

    public String rollbackFile(RepoConfig config, String file)  {
        throw new RuntimeException("rollback not implemented");
    }

    public String commit(RepoConfig config)  {

        throw new RuntimeException("rollback not implemented");
    }

    public List<String> diff(RepoConfig config){

        throw new RuntimeException("rollback not implemented");
    }

    private void setCreds(TransportCommand command, RepoConfig config)  {
        Map<String, String> creds = config.creds;
        if (creds.containsKey(Username)) {
            CredentialsProvider credsProvider =  new UsernamePasswordCredentialsProvider
                    (creds.get(Username), creds.get(Password));
            command.setCredentialsProvider(credsProvider);
        } else if (creds.containsKey(Ssh)) {
            String privateKey = creds.get(Ssh);
            byte[] privateKeyBytes = privateKey.getBytes(StandardCharsets.UTF_8);

            byte []passphraseBytes;
            if (creds.containsKey(Passphrase)) {
                passphraseBytes = creds.get(Passphrase).getBytes(StandardCharsets.UTF_8);
            }else {
                passphraseBytes=null;
            }
            String passphrase = creds.get(Password);
            SshSessionFactory sshSessionFactory = new   JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    // Additional configurations if needed
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
        }
    }
}
