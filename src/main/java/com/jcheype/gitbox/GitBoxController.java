package com.jcheype.gitbox;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Julien Cheype
 * Date: 7/29/11
 * Time: 6:50 PM
 */
public class GitBoxController implements NotificationListener, GitBoxListener {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private final NotificationClient notificationClient;
    private final GitBox gitBox;
    private final GitBoxFileListener gitBoxFileListener;

    public GitBoxController(NotificationClient notificationClient, GitBox gitBox) {
        this.notificationClient = notificationClient;
        this.gitBox = gitBox;

        notificationClient.addListener(this);
        gitBox.addListener(this);
        gitBoxFileListener = new GitBoxFileListener(gitBox);
    }

    @Override
    public void onNotification(JsonNode rootNode) {
        try {
            logger.info("pull remote change");
            gitBox.pull();
        } catch (Exception e) {
            logger.error("cannot make pull", e);
        }
    }

    @Override
    public void onUpdate() {
        logger.info("publish local change");
        notificationClient.publish();
    }

    public NotificationClient getNotificationClient() {
        return notificationClient;
    }

    public GitBox getGitBox() {
        return gitBox;
    }

    public GitBoxFileListener getGitBoxFileListener() {
        return gitBoxFileListener;
    }
}
