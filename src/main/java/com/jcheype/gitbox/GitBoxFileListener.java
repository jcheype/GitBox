package com.jcheype.gitbox;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Julien Cheype
 * Date: 7/29/11
 * Time: 1:59 PM
 */
public class GitBoxFileListener implements JNotifyListener {
    private static final Logger logger = LoggerFactory.getLogger(GitBoxFileListener.class);

    private final GitBox gitBox;
    int mask = JNotify.FILE_CREATED |
            JNotify.FILE_DELETED |
            JNotify.FILE_MODIFIED |
            JNotify.FILE_RENAMED;
    boolean watchSubtree = true;
    private int watchID = -1;

    public GitBoxFileListener(GitBox gitBox) {
        this.gitBox = gitBox;
    }

    synchronized public void start() throws JNotifyException {
        if (watchID != -1) {
            throw new IllegalArgumentException("already started");
        }
        watchID = JNotify.addWatch(gitBox.getGit().getRepository().getDirectory().getParent(), mask, watchSubtree, this);
    }

    synchronized public void stop() {
        if (watchID == -1)
            return;

        try {
            boolean res = JNotify.removeWatch(watchID);
            watchID = -1;
            if (!res) {
                logger.error("JNotify error: invalid watch ID specified.");
            }
        } catch (JNotifyException e) {
            logger.error("JNotify error:", e);
        }
    }

    synchronized public void fileRenamed(int wd, String rootPath, String oldName,
                                         String newName) {
        if (oldName.startsWith(".git")) {
            return;
        }
        String msg = "renamed: " + oldName + " -> " + newName;
        logger.debug(msg);
        gitBox.updated();
    }

    synchronized public void fileModified(int wd, String rootPath, String name) {
        if (name.startsWith(".git")) {
            return;
        }
        String msg = "modified: " + name;
        logger.debug(msg);
        gitBox.updated();
    }

    synchronized public void fileDeleted(int wd, String rootPath, String name) {
        if (name.startsWith(".git")) {
            return;
        }
        String msg = "deleted: " + name;
        logger.debug(msg);
        gitBox.updated();
    }

    synchronized public void fileCreated(int wd, String rootPath, String name) {
        if (name.startsWith(".git")) {
            return;
        }
        String msg = "created: " + name;
        logger.debug(msg);
        gitBox.updated();
    }
}
