package com.termux.app.sky;

public class VideoItem {
    private String title;
    private String url;

    public VideoItem(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
