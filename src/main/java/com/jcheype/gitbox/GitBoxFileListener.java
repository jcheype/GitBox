package com.jcheype.gitbox;

import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Julien Cheype
 * Date: 7/29/11
 * Time: 1:59 PM
 */
public class GitBoxFileListener implements JNotifyListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GitBoxFileListener.class);

    private final GitBox gitBox;

    public GitBoxFileListener(GitBox gitBox) {
        this.gitBox = gitBox;
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
