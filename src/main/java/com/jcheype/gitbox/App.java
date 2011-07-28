package com.jcheype.gitbox;

import com.google.common.io.Closeables;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;
import org.apache.commons.cli.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import javax.management.Notification;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hello world!
 */
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

        String remoteSha1 = AeSimpleSHA1.SHA1(remote);
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
        // sleep a little, the application will exit if you
        // don't (watching is asynchronous), depending on your
        // application, this may not be required
        Thread.sleep(Long.MAX_VALUE);

        // to remove watch the watch
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
