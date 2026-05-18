package com.termux.sky.txplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3UParser {

    /**
     * Parses M3U content and extracts metadata, DRM info, cookies, and stream URLs.
     */
    public static List<ChannelModel> parse(String content) {
        List<ChannelModel> channels = new ArrayList<>();

        String[] blocks = content.split("(?=#EXTINF)");

        for (String block : blocks) {
            String trimmedBlock = block.trim();
            if (trimmedBlock.isEmpty() || !trimmedBlock.contains("#EXTINF")) continue;

            ChannelModel current = new ChannelModel();
            String[] lines = trimmedBlock.split("\n");

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("#EXTINF")) {
                    if (line.contains(",")) {
                        current.name = line.substring(line.lastIndexOf(",") + 1).trim();
                    }
                    current.id = getAttribute(line, "tvg-id");
                    current.logo = getAttribute(line, "tvg-logo");
                    current.group = getAttribute(line, "group-title");
                    current.language = getAttribute(line, "tvg-language");
                    current.type = getAttribute(line, "tvg-type");
                } else if (line.startsWith("#KODIPROP:inputstream.adaptive.license_type")) {
                    current.licenseType = getValue(line);
                } else if (line.startsWith("#KODIPROP:inputstream.adaptive.license_key")) {
                    current.licenseKey = getValue(line);
                } else if (line.startsWith("#KODIPROP:inputstream.adaptive.manifest_type")) {
                    current.manifestType = getValue(line);
                } else if (line.startsWith("#EXTVLCOPT:http-user-agent")) {
                    current.userAgent = getValue(line);
                } else if (line.startsWith("#EXTVLCOPT:http-cookie")) {
                    current.cookie = getValue(line);
                } else if (line.startsWith("http")) {
                    current.url = line;

                    if (line.contains("|")) {
                        String[] parts = line.split("\\|");
                        for (int i = 1; i < parts.length; i++) {
                            String part = parts[i].trim();
                            if (part.toLowerCase().startsWith("cookie=")) {
                                current.cookie = part.substring(7);
                            } else if (part.toLowerCase().startsWith("user-agent=")) {
                                current.userAgent = part.substring(11);
                            }
                        }
                    }
                }
            }

            if (current.url != null && !current.url.isEmpty()) {
                channels.add(current);
            }
        }
        return channels;
    }

    private static String getValue(String line) {
        if (line.contains("=")) {
            return line.substring(line.indexOf("=") + 1).trim();
        }
        return "";
    }

    private static String getAttribute(String line, String key) {
        Pattern pattern = Pattern.compile(key + "=\"(.*?)\"");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    // ==========================================
    // SHAREDPREFERENCES
    // ==========================================

    public static void saveToPrefs(Context context, String port, List<ChannelModel> channels) {
        SharedPreferences prefs = context.getSharedPreferences("port_" + port, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("count", channels.size());

        for (int i = 0; i < channels.size(); i++) {
            ChannelModel ch = channels.get(i);
            String prefix = "ch_" + i + "_";

            editor.putString(prefix + "name", ch.name);
            editor.putString(prefix + "id", ch.id);
            editor.putString(prefix + "url", ch.url);
            editor.putString(prefix + "logo", ch.logo);
            editor.putString(prefix + "group", ch.group);
            editor.putString(prefix + "lang", ch.language);
            editor.putString(prefix + "type", ch.type);
            editor.putString(prefix + "lic_type", ch.licenseType);
            editor.putString(prefix + "lic_key", ch.licenseKey);
            editor.putString(prefix + "man_type", ch.manifestType);
            editor.putString(prefix + "ua", ch.userAgent);
            editor.putString(prefix + "cookie", ch.cookie); // Save Cookie string
            editor.putBoolean(prefix + "is_fav", ch.isFavorite);
        }
        editor.apply();
    }

    public static boolean existsInPrefs(Context context, String port) {
        SharedPreferences prefs = context.getSharedPreferences("port_" + port, Context.MODE_PRIVATE);
        return prefs.contains("count") && prefs.getInt("count", 0) > 0;
    }

    public static List<ChannelModel> getFromPrefs(Context context, String port) {
        List<ChannelModel> channels = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences("port_" + port, Context.MODE_PRIVATE);

        int count = prefs.getInt("count", 0);

        for (int i = 0; i < count; i++) {
            ChannelModel ch = new ChannelModel();
            String prefix = "ch_" + i + "_";

            ch.name = prefs.getString(prefix + "name", "");
            ch.id = prefs.getString(prefix + "id", "");
            ch.url = prefs.getString(prefix + "url", "");
            ch.logo = prefs.getString(prefix + "logo", "");
            ch.group = prefs.getString(prefix + "group", "");
            ch.language = prefs.getString(prefix + "lang", "");
            ch.type = prefs.getString(prefix + "type", "");
            ch.licenseType = prefs.getString(prefix + "lic_type", "");
            ch.licenseKey = prefs.getString(prefix + "lic_key", "");
            ch.manifestType = prefs.getString(prefix + "man_type", "");
            ch.userAgent = prefs.getString(prefix + "ua", "");
            ch.cookie = prefs.getString(prefix + "cookie", ""); // Retrieve Cookie string
            ch.isFavorite = prefs.getBoolean(prefix + "is_fav", false);

            if (ch.url != null && !ch.url.isEmpty()) {
                channels.add(ch);
            }
        }
        return channels;
    }

    // ==========================================
    // JSON FILE CACHE
    // ==========================================

    public static boolean existsInCache(Context context, String port) {
        File file = new File(context.getCacheDir(), "playlist_" + port + ".json");
        return file.exists();
    }

    public static void saveToCache(Context context, String port, List<ChannelModel> channels) {
        File file = new File(context.getCacheDir(), "playlist_" + port + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            JSONArray array = new JSONArray();
            for (ChannelModel ch : channels) {
                JSONObject obj = new JSONObject();
                obj.put("name", ch.name != null ? ch.name : "");
                obj.put("id", ch.id != null ? ch.id : "");
                obj.put("url", ch.url != null ? ch.url : "");
                obj.put("logo", ch.logo != null ? ch.logo : "");
                obj.put("group", ch.group != null ? ch.group : "");
                obj.put("lang", ch.language != null ? ch.language : "");
                obj.put("type", ch.type != null ? ch.type : "");
                obj.put("originPort", ch.originPort != null ? ch.originPort : "");

                // DRM Info & Headers
                obj.put("lic_type", ch.licenseType != null ? ch.licenseType : "");
                obj.put("lic_key", ch.licenseKey != null ? ch.licenseKey : "");
                obj.put("man_type", ch.manifestType != null ? ch.manifestType : "");
                obj.put("ua", ch.userAgent != null ? ch.userAgent : "");
                obj.put("cookie", ch.cookie != null ? ch.cookie : "");

                obj.put("is_fav", ch.isFavorite);

                array.put(obj);
            }
            writer.write(array.toString());
        } catch (Exception e) {
            Log.e("M3UParser", "Error saving playlist cache", e);
        }
    }

    public static List<ChannelModel> getFromCache(Context context, String port) {
        List<ChannelModel> channels = new ArrayList<>();
        File file = new File(context.getCacheDir(), "playlist_" + port + ".json");

        if (!file.exists()) return channels;

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ChannelModel ch = new ChannelModel();

                ch.name = obj.optString("name", "");
                ch.id = obj.optString("id", "");
                ch.url = obj.optString("url", "");
                ch.logo = obj.optString("logo", "");
                ch.group = obj.optString("group", "");
                ch.language = obj.optString("lang", "");
                ch.type = obj.optString("type", "");
                ch.originPort = obj.optString("originPort", "");

                // DRM Info & Headers
                ch.licenseType = obj.optString("lic_type", "");
                ch.licenseKey = obj.optString("lic_key", "");
                ch.manifestType = obj.optString("man_type", "");
                ch.userAgent = obj.optString("ua", "");
                ch.cookie = obj.optString("cookie", ""); // Pull out from JSON cache layer

                ch.isFavorite = obj.optBoolean("is_fav", false);

                if (ch.url != null && !ch.url.isEmpty()) {
                    channels.add(ch);
                }
            }
        } catch (Exception e) {
            Log.e("M3UParser", "Error reading playlist cache", e);
        }
        return channels;
    }
}
