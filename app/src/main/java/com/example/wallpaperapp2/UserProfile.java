package com.example.wallpaperapp2;

public class UserProfile {
    public String firstName;
    public String lastName;
    public String bio;
    public String profilePhotoUrl;
    public String coverImageUrl;
    public int coverWallpaperId;
    public long updatedAt;

    public UserProfile() {
        firstName = "";
        lastName = "";
        bio = "";
        profilePhotoUrl = "";
        coverImageUrl = "";
        coverWallpaperId = -1;
        updatedAt = 0L;
    }

    public String getDisplayName(String fallbackEmail) {
        String fullName = (safe(firstName) + " " + safe(lastName)).trim();
        if (!fullName.isEmpty()) return fullName;
        return fallbackEmail == null || fallbackEmail.trim().isEmpty() ? "Wallpaper User" : fallbackEmail;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
