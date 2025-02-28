package org.abx.repository.controller;

import org.abx.repository.engine.GitRepositoryEngine;
import org.abx.repository.engine.LocalRepositoryEngine;
import org.abx.repository.engine.RepositoryEngine;
import org.abx.repository.model.RepoConfig;
import org.abx.repository.model.RepoReq;

import java.io.File;

public class RepositoryProcessor extends Thread {
    private final String dir;
    private final RepositoryController controller;

    private final GitRepositoryEngine gitEngine;
    private final LocalRepositoryEngine localEngine;

    public RepositoryProcessor(String dir, RepositoryController controller) {
        this.dir = dir;
        this.controller = controller;
        gitEngine = new GitRepositoryEngine();
        gitEngine.setDir(dir);
        localEngine = new LocalRepositoryEngine();
        localEngine.setDir(dir);
    }

    public boolean validate(RepoConfig config) {
        try {
            RepositoryEngine engine = getEngine(config.engine);
            return engine.validate(config);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void run() {
        for (; ; ) {
            controller.semaphore.acquireUninterruptibly();
            RepoReq repoReq = this.controller.reqs.remove();
            try {
                process(repoReq);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void process(RepoReq repoReq) throws Exception {
        String req = repoReq.req;
        RepoConfig config = repoReq.config;
        RepositoryEngine engine = getEngine(config.engine);
        switch (req) {
            case "pull":
                config.lastKnownStatus = engine.pull(config);
                break;
            case "rollback":
                config.lastKnownStatus = engine.rollbackFile(config, repoReq.files);
                break;
            case "reset":
                config.lastKnownStatus = engine.reset(config);
                break;
            case "push":
                config.lastKnownStatus = engine.push(config, repoReq.files, repoReq.pushMessage);
                break;
            case "replace":
                config.engine = config.updatedConfig.engine;
                config.url = config.updatedConfig.url;
                config.branch = config.updatedConfig.branch;
                config.creds = config.updatedConfig.creds;
                config.lastKnownStatus = engine.reset(config);
                break;
            case "diff":
                config.lastKnownStatus = engine.diff(config);
                break;
            case "remove":
                File root = new File(dir + "/" + config.user + "/" + config.name);
                if (RepositoryEngine.deleteFolder(root)) {
                    controller.dispose(config.user, config.name);
                } else {
                    config.lastKnownStatus = "Cannot delete repository.";
                }
                break;
        }
    }

    public RepositoryEngine getEngine(String engine) throws Exception {
        switch (engine) {
            case "git":
                return gitEngine;
            case "local":
                return localEngine;
            default:
                throw new Exception("Unknown engine " + engine);
        }
    }
}
