package com.termux.sky.plugins;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class PluginStorage {

    private static final String PREF = "plugins_pref";
    private static final String KEY = "plugins";

    public static void save(Context ctx, List<Plugin> list) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, new Gson().toJson(list)).apply();
    }

    public static List<Plugin> load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, null);

        if (json == null) return new ArrayList<>();

        return new Gson().fromJson(json,
            new TypeToken<List<Plugin>>(){}.getType());
    }
}
