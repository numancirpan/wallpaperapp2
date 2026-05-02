package com.example.wallpaperapp2;

public class BlogPost {
    public String id;
    public int wallpaperId;
    public String imageUrl;
    public String photographer;
    public String comment;
    public String aiCategory;
    public long createdAt;

    public BlogPost() {
        id = "";
        wallpaperId = -1;
        imageUrl = "";
        photographer = "";
        comment = "";
        aiCategory = "";
        createdAt = 0L;
    }
}
