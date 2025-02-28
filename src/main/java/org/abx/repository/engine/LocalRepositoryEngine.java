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
