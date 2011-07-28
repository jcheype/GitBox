package com.jcheype.gitbox;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;
import org.apache.commons.cli.*;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;

import java.io.File;

public class App {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(App.class);

//    static {
//        System.setProperty("java.library.path", "./binlib");
//    }

    private static NotificationClient notificationClient = null;
    private static GitBox gitBox;

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("git-box.jar [OPTION]... REPO_FOLDER", options);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("java.library.path"));
        Options options = new Options();

        options.addOption("n", "notification", true, "set notification server url");
        options.addOption("r", "remote", true, "remote repository url");
        options.addOption("c", "clone", false, "set repository to clone");
        options.addOption("h", "help", false, "print this help");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("help")) {
            showHelp(options);
            System.exit(0);
        }

        if (cmd.getArgs().length < 1) {
            System.err.println("Missing REPO_FOLDER arg");
            showHelp(options);
            System.exit(1);
        }

        if (!cmd.hasOption("remote")) {
            System.err.println("Missing remote repository url");
            showHelp(options);
            System.exit(1);
        }

        String repo = cmd.getArgs()[0];

        String remote = cmd.getOptionValue("remote");
        String remoteSha1 = AeSimpleSHA1.SHA1(remote);


        if (cmd.hasOption("clone")) {
            File repoFile = new File(repo);
            if (repoFile.exists())
                GitBox.cloneGit(remote, repoFile);
            else {
                System.err.println("REPO_FOLDER must exist");
                showHelp(options);
                System.exit(1);
            }
        }

        if (!cmd.hasOption("notification")) {
            System.err.println("Missing notification server url");
            showHelp(options);
            System.exit(1);
        }
        String notifServer = cmd.getOptionValue("notification");

        logger.info("notifServer: " + notifServer);
        gitBox = new GitBox(repo) {
            @Override
            protected void onUpdate() {
                logger.info("onUpdate");
                if (notificationClient != null) notificationClient.publish();
            }
        };

        if (!notifServer.endsWith("/")) {
            notifServer += "/";
        }
        logger.info(notifServer + remoteSha1);
        notificationClient = new NotificationClient(notifServer + remoteSha1) {
            @Override
            protected void onNotification(JsonNode rootNode) {
                String from = rootNode.path("from").getTextValue();
                logger.info("notification from: " + from);
                if (!getUuid().equals(from)) {
                    try {
                        gitBox.pull();
                    } catch (Exception e) {
                        logger.error("cannot make pull", e);
                    }
                }
            }
        };

        gitBox.start();
        notificationClient.start();

        int mask = JNotify.FILE_CREATED |
                JNotify.FILE_DELETED |
                JNotify.FILE_MODIFIED |
                JNotify.FILE_RENAMED;
        boolean watchSubtree = true;

        int watchID = JNotify.addWatch(gitBox.getGit().getRepository().getDirectory().getParent(), mask, watchSubtree, new Listener(gitBox));
        Thread.sleep(Long.MAX_VALUE);

        boolean res = JNotify.removeWatch(watchID);
        if (!res) {
            logger.error("invalid watch ID specified.");
        }
    }

    static class Listener implements JNotifyListener {
        private final GitBox gitBox;

        public Listener(GitBox gitBox) {
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
}
