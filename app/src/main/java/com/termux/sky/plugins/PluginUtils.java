package com.termux.sky.plugins;

import android.util.Log;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class PluginUtils {

    public static Plugin parse(String json) {
        try {
            JSONObject o = new JSONObject(json);

            Plugin p = new Plugin();
            p.title = o.getString("title");
            p.tool = o.has("tool") && o.getBoolean("tool");
            p.app_start_url = o.has("app_start_url") && o.getBoolean("app_start_url");
            p.repo =o.has("repo") ? o.getString("repo") : null;
            p.repo_branch =o.has("repo_branch") ? o.getString("repo_branch") : null;
            p.bin_download = o.has("bin_download") ? o.getString("bin_download") : null;
            p.port = o.getInt("port");
            p.playlist = o.has("playlist") ? o.getString("playlist") : null;
            p.server_check_url = o.has("server_check_url") ? o.getString("server_check_url") : null;
            p.watch_url = o.has("watch_url") ? o.getString("watch_url") : null;
            p.login_url = o.has("login_url") ? o.getString("login_url") : null;
            p.support_url = o.has("support_url") ? o.getString("support_url") : null;
            p.post_install_script = o.has("post_install_script") ? o.getString("post_install_script") : null;
            p.updatable = o.has("updatable") && o.getBoolean("updatable");
            p.start = o.has("start") ? o.getString("start") : null;

            return p;

        } catch (Exception e) {
            Log.d("PluginUtils", "Error: ", e);
            Log.d("PluginUtils", "JSON: "+ json);
            return null;
        }
    }

    public static boolean isRunning(String urlStr, Boolean tool) {
        if (urlStr == null || urlStr.trim().isEmpty()) return false;

        urlStr = urlStr.trim();

        if (urlStr.startsWith("content://") || urlStr.startsWith("file://") || urlStr.startsWith("/")) {
            return true;
        }

        if (Boolean.TRUE.equals(tool) && !urlStr.toLowerCase().startsWith("http")) {
            return true;
        }

        HttpURLConnection c = null;
        try {
            if (urlStr.contains(" ")) {
                urlStr = urlStr.replace(" ", "%20");
            }

            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();

            c.setConnectTimeout(1500);
            c.setReadTimeout(1500);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            c.setRequestMethod("HEAD");
            int code = c.getResponseCode();

            if (code == 405 || code == 403 || code == 501) {
                c.disconnect();

                c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(1500);
                c.setReadTimeout(1500);
                c.setInstanceFollowRedirects(true); // Added missing redirect fix
                c.setRequestMethod("GET");
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

                code = c.getResponseCode();
            }

            return code >= 200 && code < 400;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (c != null) {
                c.disconnect();
            }
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
