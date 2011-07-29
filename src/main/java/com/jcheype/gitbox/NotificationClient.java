package com.jcheype.gitbox;

import com.google.common.io.Closeables;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mush
 * Date: 7/27/11
 * Time: 10:53 PM
 */
public class NotificationClient implements Runnable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NotificationClient.class);
    private final String uuid = UUID.randomUUID().toString();
    private final String notifUrl;
    private final Thread thread;
    private final List<NotificationListener> listeners = new ArrayList<NotificationListener>();

    public NotificationClient(String notifUrl) {
        this.notifUrl = notifUrl;
        logger.info("NAME: " + uuid);
        thread = new Thread(this);
    }

    public void start() {
        thread.start();
    }

    private void waitNotification() throws InterruptedException {
        HttpClient httpclient = new DefaultHttpClient();

        HttpGet httpget = new HttpGet(notifUrl);
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        InputStream instream = null;
        try {
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            instream = entity.getContent();
            JsonNode rootNode = mapper.readValue(instream, JsonNode.class);
            onNotification(rootNode);
        } catch (Exception e) {
            logger.error("notification server is unreachable, retry in 10s");
            logger.debug("details", e);
            Thread.sleep(10000);
        } finally {
            Closeables.closeQuietly(instream);
            httpclient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void run() {
        try {
            while (!thread.isInterrupted()) {
                waitNotification();
            }
        } catch (InterruptedException e) {
            logger.info("client thread interrupted");
        }
    }

    public void publish() {
        logger.debug("publish:" + notifUrl);
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost post = new HttpPost(notifUrl);
        try {
            post.setEntity(new StringEntity("{\"from\" : \"" + uuid + "\"}", "UTF-8"));
            HttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() >= 400) {
                logger.error("error while posting notification: " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onNotification(JsonNode rootNode) {
        String from = rootNode.path("from").getTextValue();
        if (!getUuid().equals(from)) {
            logger.info("notification from: " + from);
            for (NotificationListener listener : listeners) {
                listener.onNotification(rootNode);
            }
        } else {
            logger.info("notification from: me");
        }

    }

    public void addListener(NotificationListener listener) {
        if (this.thread.isAlive()) {
            throw new IllegalArgumentException("cannot add listener once started");
        }
        listeners.add(listener);
    }

    public String getUuid() {
        return uuid;
    }

    public void stop() {
        thread.interrupt();
    }
}
