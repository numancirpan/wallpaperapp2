package com.example.wallpaperapp2;

public class Wallpaper {
    public int id;
    public int imageRes;
    public boolean isFavorite;
    public String title;

    public String aiCategory;
    public String aiLabels;

    public Wallpaper(int id, int imageRes, String title) {
        this.id = id;
        this.imageRes = imageRes;
        this.title = title;
        this.isFavorite = false;
        this.aiCategory = "";
        this.aiLabels = "";
    }
}