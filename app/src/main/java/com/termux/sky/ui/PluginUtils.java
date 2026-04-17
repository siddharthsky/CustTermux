package com.termux.sky.ui;

import android.util.Log;

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
            p.repo =o.has("repo") ? o.getString("repo") : null;
            p.repo_branch =o.has("repo_branch") ? o.getString("repo_branch") : null;
            p.bin_download = o.has("bin_download") ? o.getString("bin_download") : null;
            p.port = o.getInt("port");
            p.playlist = o.getString("playlist");
            p.server_check_url = o.has("server_check_url") ? o.getString("server_check_url") : null;
            p.watch_url = o.has("watch_url") ? o.getString("watch_url") : null;
            p.login_url = o.has("login_url") ? o.getString("login_url") : null;
            p.post_install_script = o.has("post_install_script") ? o.getString("post_install_script") : null;
            p.start = o.getString("start");

            return p;

        } catch (Exception e) {
            Log.d("PluginUtils", "Error: ", e);
            return null;
        }
    }

    public static boolean isRunning(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();

            c.setConnectTimeout(1500);
            c.setReadTimeout(1500);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Try HEAD first
            c.setRequestMethod("HEAD");

            int code = c.getResponseCode();

            // fallback if HEAD not supported
            if (code == 405 || code == 403) {
                c.disconnect();

                c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(1500);
                c.setReadTimeout(1500);
                c.setRequestMethod("GET");
                c.setRequestProperty("User-Agent", "Mozilla/5.0");

                code = c.getResponseCode();
            }

            return code >= 200 && code < 400;

        } catch (Exception e) {
            return false;
        }
    }

    public static void run(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh","-c",cmd});
        } catch (Exception e) {
            Log.d("PluginUtils", "Error: ", e);
        }
    }
}
