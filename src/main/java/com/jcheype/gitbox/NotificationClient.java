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

import java.io.InputStream;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mush
 * Date: 7/27/11
 * Time: 10:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class NotificationClient implements Runnable {
    private final String uuid = UUID.randomUUID().toString();
    private final String notifUrl;

    public NotificationClient(String notifUrl) {
        this.notifUrl = notifUrl;
        System.out.println("NAME: " + uuid);
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (true) {
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
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                Closeables.closeQuietly(instream);
                httpclient.getConnectionManager().shutdown();
            }
        }
    }

    public void publish() {
        System.out.println("publish:" + notifUrl);
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost post = new HttpPost(notifUrl);
        try {
            post.setEntity(new StringEntity("{\"from\" : \"" + uuid + "\"}", "UTF-8"));
            HttpResponse response = httpclient.execute(post);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    protected void onNotification(JsonNode rootNode) {

    }

    public String getUuid() {
        return uuid;
    }
}
