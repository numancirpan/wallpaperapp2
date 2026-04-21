package com.example.wallpaperapp2;

public class Wallpaper {
    public int imageRes;
    public boolean isFavorite;
    public String title;
    public String category;

    public Wallpaper(int imageRes, String title, String category) {
        this.imageRes = imageRes;
        this.title = title;
        this.category = category;
        this.isFavorite = false;
    }
}