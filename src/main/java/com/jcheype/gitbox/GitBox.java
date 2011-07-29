package com.jcheype.gitbox;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * User: mush
 * Date: 7/27/11
 * Time: 10:37 PM
 */
public class GitBox {
    private static final Logger logger = LoggerFactory.getLogger(GitBox.class);

    private final Git git;
    private final AtomicLong lastChange = new AtomicLong(0);
    private final AtomicBoolean shouldUpdate = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);


    public GitBox(String path) throws Exception {
        RepositoryBuilder builder = new RepositoryBuilder();
        Repository repository = builder.setWorkTree(new File(path))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(new File(path)) // scan up the file system tree
                .build();

        git = new Git(repository);
        pull();
    }

    public boolean checkGit() throws Exception {
        logger.debug("checkGit");

        if (!(git.status().call().getAdded().isEmpty() &&
                git.status().call().getChanged().isEmpty() &&
                git.status().call().getMissing().isEmpty() &&
                git.status().call().getModified().isEmpty() &&
                git.status().call().getRemoved().isEmpty() &&
                git.status().call().getUntracked().isEmpty())) {
            logger.info("change detected");
            git.add().addFilepattern(".").call();
            git.commit().setAll(true).setMessage("change").call();
            git.pull().call();
            git.push().call();
            return true;
        }
        return false;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long last = System.currentTimeMillis() - lastChange.get();
                if ((last > 2000 && shouldUpdate.getAndSet(false)) || last > 500000) {
                    try {
                        if (checkGit()) {
                            onUpdate();
                        }
                    } catch (Exception e) {
                        logger.error("error on checkgit: ", e);
                    }
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void pull() throws RefNotFoundException, DetachedHeadException, WrongRepositoryStateException, InvalidRemoteException, InvalidConfigurationException, CanceledException {
        final PullResult call = git.pull().call();
        logger.debug("pull:" + call.toString());
    }

    public static void cloneGit(String url, File repoPath) {
        Git.cloneRepository().setURI(url).setDirectory(repoPath).call();
    }

    public void updated() {
        lastChange.set(System.currentTimeMillis());
        shouldUpdate.set(true);
    }

    protected void onUpdate() {
    }

    public Git getGit() {
        return git;
    }
}
