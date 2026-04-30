package com.example.wallpaperapp2;

public class Wallpaper {
    public int id;
    public int imageRes;
    public String imageUrl;
    public boolean isFavorite;
    public String title;

    public String aiCategory;
    public String aiLabels;

    public Wallpaper(int id, int imageRes, String title) {
        this(id, imageRes, "", title);
    }

    public Wallpaper(int id, String imageUrl, String title) {
        this(id, 0, imageUrl, title);
    }

    public Wallpaper(int id, int imageRes, String imageUrl, String title) {
        this.id = id;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.title = title;
        this.isFavorite = false;
        this.aiCategory = "";
        this.aiLabels = "";
    }

    public boolean hasRemoteImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }
}