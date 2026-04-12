package com.termux.sky.ui;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class PluginUtils {

    public static Plugin parse(String json) {
        try {
            JSONObject o = new JSONObject(json);

            Plugin p = new Plugin();
            p.title = o.getString("title");
            p.repo = o.getString("repo");
            p.bin_download = o.getString("bin_download");
            p.port = o.getInt("port");
            p.playlist = o.getString("playlist");
            p.start = o.getString("start");

            return p;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isRunning(String playlist) {
        try {
            URL url = new URL(playlist);
            URI uri = url.toURI();

            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            String baseUrl = scheme + "://" + host;
            if (port != -1) {
                baseUrl += ":" + port;
            }
            baseUrl += "/";

            URL checkUrl = new URL(baseUrl);

            HttpURLConnection c = (HttpURLConnection) checkUrl.openConnection();
            c.setConnectTimeout(1000);
            c.connect();

            return c.getResponseCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    public static void run(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh","-c",cmd});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
