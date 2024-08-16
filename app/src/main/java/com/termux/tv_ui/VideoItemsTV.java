package com.termux.tv_ui;


public class VideoItemsTV {
    private String title;
    private static String url;

    public VideoItemsTV(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public static String getUrl() {
        return url;
    }
}
