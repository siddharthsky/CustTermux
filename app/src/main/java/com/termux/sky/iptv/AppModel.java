package com.termux.sky.iptv;

public class AppModel {
    public String appName;
    public String packageName;
    public String activityName;

    public AppModel(String appName, String packageName, String activityName) {
        this.appName = appName;
        this.packageName = packageName;
        this.activityName = activityName;
    }
}
