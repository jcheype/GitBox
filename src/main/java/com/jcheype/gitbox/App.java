package com.jcheype.gitbox;

import org.apache.commons.cli.*;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.LoggerFactory;

import java.io.File;

public class App {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(App.class);
    private static GitBoxController gitBoxController;

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
            if (repoFile.exists()) {
                GitBox.cloneGit(remote, repoFile, new GitBoxCredentialsProvider());
            } else {
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
        if (!notifServer.endsWith("/")) {
            notifServer += "/";
        }
        logger.info("notification URL: " + notifServer + remoteSha1);

        GitBox gitBox = new GitBox(repo);
        NotificationClient notificationClient = new NotificationClient(notifServer + remoteSha1);

        gitBoxController = new GitBoxController(notificationClient, gitBox);
        gitBoxController.getGitBox().start();
        gitBoxController.getNotificationClient().start();
        gitBoxController.getGitBoxFileListener().start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                gitBoxController.getGitBox().stop();
                gitBoxController.getNotificationClient().stop();
                gitBoxController.getGitBoxFileListener().stop();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }
}
