package com.jcheype.gitbox;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;
import org.apache.commons.cli.*;
import org.codehaus.jackson.JsonNode;

public class App {
    static {
        System.setProperty("java.library.path", ".");
    }

    private static NotificationClient notificationClient = null;

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("git-box.jar [OPTION]... REPO_FOLDER", options);
    }

    public static void main(String[] args) throws Exception {
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
            System.out.println("Missing REPO_FOLDER arg");
            showHelp(options);
            System.exit(1);
        }

        if (!cmd.hasOption("remote")) {
            System.out.println("Missing remote repository url");
            showHelp(options);
            System.exit(1);
        }

        String repo = cmd.getArgs()[0];

        String remote = cmd.getOptionValue("remote");
        String remoteSha1 = AeSimpleSHA1.SHA1(remote);


        if (cmd.hasOption("clone")) {
            GitBox.cloneGit(remote, repo);
        }

        if (!cmd.hasOption("notification")) {
            System.out.println("Missing notification server url");
            showHelp(options);
            System.exit(1);
        }
        String notifServer = cmd.getOptionValue("notification");

        System.out.println("notifServer: " + notifServer);
        final GitBox gitBox = new GitBox(repo) {
            @Override
            protected void onUpdate() {
                System.out.println("onUpdate");
                if (notificationClient != null) notificationClient.publish();
            }
        };

        if(!notifServer.endsWith("/")){
            notifServer += "/";
        }
        System.out.println(notifServer + remoteSha1);
        notificationClient = new NotificationClient(notifServer + remoteSha1) {
            @Override
            protected void onNotification(JsonNode rootNode) {
                String from = rootNode.path("from").getTextValue();
                System.out.println("notification from: " + from);
                if (!getUuid().equals(from)) {
                    try {
                        gitBox.pull();
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        };

        int mask = JNotify.FILE_CREATED |
                JNotify.FILE_DELETED |
                JNotify.FILE_MODIFIED |
                JNotify.FILE_RENAMED;
        boolean watchSubtree = true;

        int watchID = JNotify.addWatch(gitBox.getGit().getRepository().getDirectory().getParent(), mask, watchSubtree, new Listener(gitBox));
        Thread.sleep(Long.MAX_VALUE);

        boolean res = JNotify.removeWatch(watchID);
        if (!res) {
            System.out.println("invalid watch ID specified.");
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
            System.out.println(msg);
            gitBox.updated();
        }

        synchronized public void fileModified(int wd, String rootPath, String name) {
            if (name.startsWith(".git")) {
                return;
            }
            String msg = "modified: " + name;
            System.out.println(msg);
            gitBox.updated();
        }

        synchronized public void fileDeleted(int wd, String rootPath, String name) {
            if (name.startsWith(".git")) {
                return;
            }
            String msg = "deleted: " + name;
            System.out.println(msg);
            gitBox.updated();
        }

        synchronized public void fileCreated(int wd, String rootPath, String name) {
            if (name.startsWith(".git")) {
                return;
            }
            String msg = "created: " + name;
            System.out.println(msg);
            gitBox.updated();
        }
    }
}
