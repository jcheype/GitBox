package com.jcheype.gitbox;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * User: mush
 * Date: 7/27/11
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class GitBox {
    private final Git git;
    public final static AtomicLong lastChange = new AtomicLong(0);
    public final static AtomicBoolean shouldUpdate = new AtomicBoolean(false);
    private final static Timer timer = new Timer();


    public GitBox(String path) throws Exception {
        RepositoryBuilder builder = new RepositoryBuilder();
        Repository repository = builder.setWorkTree(new File(path))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(new File(path)) // scan up the file system tree
                .build();

        git = new Git(repository);
        pull();
        initTimer();
    }



    public boolean checkGit() throws Exception {
        System.out.println("checkGit");

        if (!(git.status().call().getAdded().isEmpty() &&
                git.status().call().getChanged().isEmpty() &&
                git.status().call().getMissing().isEmpty() &&
                git.status().call().getModified().isEmpty() &&
                git.status().call().getRemoved().isEmpty() &&
                git.status().call().getUntracked().isEmpty())) {
            System.out.println("change");
            git.add().addFilepattern(".").call();
            git.commit().setAll(true).setMessage("change").call();
            git.pull().call();
            git.push().call();
            return true;
        }
        return false;
    }

    private void initTimer() throws IOException {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if ((System.currentTimeMillis() - lastChange.get()) > 2000 && shouldUpdate.getAndSet(false)) {
                    try {
                        if (checkGit()){
                            onUpdate();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        };

        timer.scheduleAtFixedRate(task, 1000, 1000);
    }

    public void pull() throws RefNotFoundException, DetachedHeadException, WrongRepositoryStateException, InvalidRemoteException, InvalidConfigurationException, CanceledException {
        final PullResult call = git.pull().call();
        System.out.println("pull:" + call.toString());
    }

    public static void cloneGit(String url, String repoPath){
        Git.cloneRepository().setURI(url).setDirectory(new File(repoPath)).call();
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
