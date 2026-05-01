package com.example.wallpaperapp2;

import android.content.Context;

public class CategoryDisplayMapper {

    public static String toDisplayName(Context context, String category) {
        if (category == null || category.trim().isEmpty()) {
            return context.getString(R.string.uncategorized);
        }

        String normalized = category.trim().toLowerCase();
        switch (normalized) {
            case "workspace":
                return context.getString(R.string.category_workspace);
            case "cafe":
                return context.getString(R.string.category_cafe);
            case "interior":
                return context.getString(R.string.category_interior);
            case "beach":
                return context.getString(R.string.category_beach);
            case "water scenes":
                return context.getString(R.string.category_water_scenes);
            case "nature":
                return context.getString(R.string.category_nature);
            case "urban":
                return context.getString(R.string.category_urban);
            case "architecture":
                return context.getString(R.string.category_architecture);
            case "vehicles":
                return context.getString(R.string.category_vehicles);
            case "animals":
                return context.getString(R.string.category_animals);
            case "texture":
                return context.getString(R.string.category_texture);
            case "lights":
                return context.getString(R.string.category_lights);
            case "abstract":
                return context.getString(R.string.category_abstract);
            case "art":
                return context.getString(R.string.category_art);
            case "people":
                return context.getString(R.string.category_people);
            case "space":
                return context.getString(R.string.category_space);
            case "uncategorized":
                return context.getString(R.string.uncategorized);
            case "analyzing":
                return context.getString(R.string.analyzing);
            default:
                return category;
        }
    }
}
