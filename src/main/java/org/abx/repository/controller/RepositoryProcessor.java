package org.abx.repository.controller;

import org.abx.repository.engine.GitRepositoryEngine;
import org.abx.repository.engine.RepositoryEngine;
import org.abx.repository.model.RepoConfig;
import org.abx.repository.model.RepoReq;

public class RepositoryProcessor extends Thread {
    private final RepositoryController controller;

    private final GitRepositoryEngine gitEngine;
    public RepositoryProcessor(String dir,RepositoryController controller) {
        this.controller = controller;
        gitEngine = new GitRepositoryEngine();
        gitEngine.setDir(dir);
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
        RepositoryEngine engine = getEngine(config);
        switch (req) {
            case "update":
                config.lastKnownStatus = engine.update(config);
                break;
            case "push":
                config.lastKnownStatus =  engine.push(config);
                break;
            case "replace":
                config.engine=config.updatedConfig.engine;
                config.url = config.updatedConfig.url;
                config.creds = config.updatedConfig.creds;
                config.lastKnownStatus = "Updating";
                config.lastKnownStatus =  engine.update(config);
                break;
        }
    }

    private RepositoryEngine getEngine(RepoConfig config) throws Exception {
        switch (config.engine) {
            case "git":
                return gitEngine;
            default:
                throw new Exception("Unknown engine " + config.engine);
        }
    }
}
