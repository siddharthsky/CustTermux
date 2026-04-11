package com.termux.sky.ui;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class PluginUtils {

    public static Plugin parse(String json) {
        try {
            JSONObject o = new JSONObject(json);

            Plugin p = new Plugin();
            p.title = o.getString("title");
            p.repo = o.getString("repo");
            p.port = o.getInt("port");
            p.playlist = o.getString("playlist");
            p.start = o.getString("start");

            return p;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 🔥 Check running (like curl)
    public static boolean isRunning(String playlist) {
        try {
            String base = playlist.replace("/playlist.php", "");
            URL url = new URL(base);

            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(1000);
            c.connect();

            return c.getResponseCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    // 🔥 Run shell command
    public static void run(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh","-c",cmd});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
