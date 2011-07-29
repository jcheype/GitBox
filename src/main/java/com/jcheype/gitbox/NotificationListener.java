package com.jcheype.gitbox;

import org.codehaus.jackson.JsonNode;

/**
 * Created by IntelliJ IDEA.
 * User: Julien Cheype
 * Date: 7/29/11
 * Time: 6:52 PM
 */
public interface NotificationListener {
    void onNotification(JsonNode rootNode);
}
