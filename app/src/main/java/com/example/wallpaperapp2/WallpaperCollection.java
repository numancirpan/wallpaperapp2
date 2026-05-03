package com.example.wallpaperapp2;

import java.util.ArrayList;
import java.util.List;

public class WallpaperCollection {
    public String id;
    public String name;
    public List<Wallpaper> wallpapers;
    public long updatedAt;

    public WallpaperCollection() {
        id = "";
        name = "";
        wallpapers = new ArrayList<>();
        updatedAt = 0L;
    }
}
