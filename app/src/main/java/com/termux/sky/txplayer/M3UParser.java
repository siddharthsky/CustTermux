package com.termux.sky.txplayer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3UParser {

    /**
     * Parses M3U content and extracts metadata, DRM info, and stream URLs.
     */
    public static List<ChannelModel> parse(String content) {
        List<ChannelModel> channels = new ArrayList<>();

        // Regex split: Splits at #EXTINF but keeps the delimiter.
        // This handles messy formatting where URLs and Tags are on the same line.
        String[] blocks = content.split("(?=#EXTINF)");

        for (String block : blocks) {
            String trimmedBlock = block.trim();
            if (trimmedBlock.isEmpty() || !trimmedBlock.contains("#EXTINF")) continue;

            ChannelModel current = new ChannelModel();
            String[] lines = trimmedBlock.split("\n");

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("#EXTINF")) {
                    // Extract name (everything after the last comma)
                    if (line.contains(",")) {
                        current.name = line.substring(line.lastIndexOf(",") + 1).trim();
                    }

                    // Extract Standard Attributes
                    current.id = getAttribute(line, "tvg-id");
                    current.logo = getAttribute(line, "tvg-logo");
                    current.group = getAttribute(line, "group-title");

                    // Extract Extended Attributes
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
                } else if (line.startsWith("http")) {
                    // Captures the full URL (including pipe-delimited headers)
                    current.url = line;
                }
            }

            // Only add if we actually found a stream URL
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
        // Regex to find key="value"
        Pattern pattern = Pattern.compile(key + "=\"(.*?)\"");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * Saves the list of channels to SharedPreferences.
     */
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

            // DRM Info
            editor.putString(prefix + "lic_type", ch.licenseType);
            editor.putString(prefix + "lic_key", ch.licenseKey);
            editor.putString(prefix + "man_type", ch.manifestType);
            editor.putString(prefix + "ua", ch.userAgent);
        }

        editor.apply();
    }

    /**
     * Checks if data for a specific port already exists in SharedPreferences.
     */
    public static boolean existsInPrefs(Context context, String port) {
        SharedPreferences prefs = context.getSharedPreferences("port_" + port, Context.MODE_PRIVATE);
        // If "count" exists and is greater than 0, we have data.
        return prefs.contains("count") && prefs.getInt("count", 0) > 0;
    }

    /**
     * Retrieves the list of channels from SharedPreferences.
     */
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

            // DRM Info
            ch.licenseType = prefs.getString(prefix + "lic_type", "");
            ch.licenseKey = prefs.getString(prefix + "lic_key", "");
            ch.manifestType = prefs.getString(prefix + "man_type", "");
            ch.userAgent = prefs.getString(prefix + "ua", "");

            // Safety check: ensure we don't add empty/broken records
            if (ch.url != null && !ch.url.isEmpty()) {
                channels.add(ch);
            }
        }
        return channels;
    }

}
