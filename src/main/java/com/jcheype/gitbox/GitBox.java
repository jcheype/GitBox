package com.jcheype.gitbox;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
    private final CredentialsProvider credentialsProvider;
    private final AtomicLong lastChange = new AtomicLong(0);
    private final AtomicBoolean shouldUpdate = new AtomicBoolean(false);
    private final List<GitBoxListener> listeners = new ArrayList<GitBoxListener>();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedule;
    private int timeout = 500000;


    public GitBox(String path, CredentialsProvider credentialsProvider) throws Exception {
        RepositoryBuilder builder = new RepositoryBuilder();
        Repository repository = builder.setWorkTree(new File(path))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(new File(path)) // scan up the file system tree
                .build();

        this.git = new Git(repository);
        this.credentialsProvider=credentialsProvider;
        pull();
    }

    public GitBox(String path) throws Exception {
        this(path, null);
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
            git.pull().setCredentialsProvider(credentialsProvider).call();
            git.push().setCredentialsProvider(credentialsProvider).call();
            return true;
        }
        return false;
    }

    public boolean isStarted(){
        return schedule != null;
    }
    synchronized public void start() {
        if (schedule != null) {
            throw new IllegalArgumentException("already started");
        }
        schedule = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                long last = System.currentTimeMillis() - lastChange.get();
                if ((last > 2000 && shouldUpdate.getAndSet(false)) || last > timeout) {
                    try {
                        if (checkGit()) {
                            for (GitBoxListener listener : listeners) {
                                listener.onUpdate();
                            }
                        }
                    } catch (Exception e) {
                        logger.error("error on checkgit: ", e);
                    }
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    synchronized public void stop() {
        if (schedule != null) {
            schedule.cancel(false);
            schedule = null;
        }
    }

    public void pull() throws RefNotFoundException, DetachedHeadException, WrongRepositoryStateException, InvalidRemoteException, InvalidConfigurationException, CanceledException {
        final PullResult call = git.pull().setCredentialsProvider(credentialsProvider).call();
        logger.debug("pull:" + call.toString());
    }

    public static void cloneGit(String url, File repoPath) {
        cloneGit(url, repoPath, null);
    }

    public static void cloneGit(String url, File repoPath, CredentialsProvider credentialsProvider) {
        Git.cloneRepository().setCredentialsProvider(credentialsProvider).setURI(url).setDirectory(repoPath).call();
    }

    public void updated() {
        lastChange.set(System.currentTimeMillis());
        shouldUpdate.set(true);
    }

    public Git getGit() {
        return git;
    }

    public void addListener(GitBoxListener gitBoxListener) {
        if (schedule != null) {
            throw new IllegalArgumentException("cannot add listener once started");
        }
        listeners.add(gitBoxListener);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
