package com.termux.sky.plugin_parser;

public class ChannelModel {
    public String id = "";
    public String name = "";
    public String logo = "";
    public String group = "";
    public String language = "";
    public String type = "";
    public String url = "";

    // DRM & Header properties
    public String licenseType = "";
    public String licenseKey = "";
    public String manifestType = "";
    public String userAgent = "";
    public String cookie = "";

    public boolean isFavorite = false;
    public String originPort;
}
