package com.example.wallpaperapp2;

import android.content.Context;

import java.util.Locale;

public class CategoryDisplayMapper {

    public static String canonicalName(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Uncategorized";
        }

        String normalized = category.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "çalışma alanı":
            case "workspace":
            case "laptop":
            case "computer":
            case "book":
                return "Workspace";

            case "kafe":
            case "cafe":
                return "Cafe";

            case "iç mekan":
            case "interior":
            case "chair":
            case "bench":
            case "wall":
                return "Interior";

            case "moda":
            case "fashion":
            case "shoe":
            case "shoes":
            case "footwear":
            case "sneakers":
            case "foot":
            case "flesh":
                return "Fashion";

            case "sahil":
            case "beach":
            case "coast":
            case "shore":
                return "Beach";

            case "su manzaraları":
            case "water scenes":
            case "waterfall":
            case "river":
            case "lake":
                return "Water Scenes";

            case "tekne":
            case "boat":
            case "ship":
            case "watercraft":
                return "Boat";

            case "doğa":
            case "nature":
            case "branch":
            case "twig":
            case "flower":
            case "plant":
            case "tree":
            case "forest":
            case "field":
            case "prairie":
            case "insect":
                return "Nature";

            case "şehir":
            case "city":
            case "urban":
            case "street":
            case "road":
                return "Urban";

            case "mimari":
            case "architecture":
            case "building":
                return "Architecture";

            case "araçlar":
            case "vehicles":
            case "vehicle":
            case "car":
                return "Vehicles";

            case "hayvanlar":
            case "animals":
            case "cat":
            case "dog":
            case "bird":
                return "Animals";

            case "doku":
            case "texture":
            case "pattern":
            case "asphalt":
            case "surface":
                return "Texture";

            case "ışıklar":
            case "lights":
            case "bokeh":
                return "Lights";

            case "tek renk":
            case "monochrome":
                return "Monochrome";

            case "soyut":
            case "abstract":
                return "Abstract";

            case "sanat":
            case "art":
                return "Art";

            case "insanlar":
            case "people":
            case "person":
            case "portrait":
                return "People";

            case "uzay":
            case "space":
                return "Space";

            case "diğer":
            case "diger":
            case "other":
            case "kategorisiz":
            case "uncategorized":
                return "Uncategorized";

            case "analiz ediliyor":
            case "analyzing":
                return "Analyzing";

            default:
                return category.trim();
        }
    }

    public static String toDisplayName(Context context, String category) {
        String canonical = canonicalName(category);
        String normalized = canonical.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "workspace":
                return context.getString(R.string.category_workspace);
            case "cafe":
                return context.getString(R.string.category_cafe);
            case "interior":
                return context.getString(R.string.category_interior);
            case "fashion":
                return context.getString(R.string.category_fashion);
            case "beach":
                return context.getString(R.string.category_beach);
            case "water scenes":
                return context.getString(R.string.category_water_scenes);
            case "boat":
                return context.getString(R.string.category_boat);
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
            case "monochrome":
                return context.getString(R.string.category_monochrome);
            case "abstract":
                return context.getString(R.string.category_abstract);
            case "art":
                return context.getString(R.string.category_art);
            case "people":
                return context.getString(R.string.category_people);
            case "space":
                return context.getString(R.string.category_space);
            case "uncategorized":
                return context.getString(R.string.category_other);
            case "analyzing":
                return context.getString(R.string.analyzing);
            default:
                return canonical;
        }
    }
}
