package com.example.wallpaperapp2;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiLabelDisplayMapper {

    public static String toDisplayLabels(Context context, String labelsCsv) {
        if (labelsCsv == null || labelsCsv.trim().isEmpty()) {
            return context.getString(R.string.not_available);
        }

        String cleaned = labelsCsv.trim();
        boolean onDevice = cleaned.toLowerCase(Locale.ROOT).contains("(on-device)")
                || cleaned.toLowerCase(Locale.ROOT).contains("(cihaz üstü)");
        cleaned = cleaned.replace("(on-device)", "")
                .replace("(cihaz üstü)", "")
                .trim();

        if (looksLikeSystemMessage(cleaned)) {
            return cleaned;
        }

        String[] parts = cleaned.split(",");
        List<String> translated = new ArrayList<>();
        for (String part : parts) {
            String label = part.trim();
            if (label.isEmpty()) continue;
            translated.add(translateLabel(context, label));
        }

        String result = translated.isEmpty()
                ? context.getString(R.string.visual_wallpaper_content)
                : String.join(", ", translated);

        if (onDevice) {
            result = context.getString(R.string.on_device_suffix, result);
        }
        return result;
    }

    private static boolean looksLikeSystemMessage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("gemini http")
                || lower.contains("api key")
                || lower.contains("failed")
                || lower.contains("progress")
                || lower.contains("cache")
                || lower.contains("prepayment")
                || lower.contains("quota");
    }

    private static String translateLabel(Context context, String label) {
        String language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        if (!"tr".equals(language)) {
            return label;
        }

        switch (label.toLowerCase(Locale.ROOT)) {
            case "cup": return "Bardak";
            case "desk": return "Masa";
            case "table": return "Masa";
            case "tableware": return "Sofra Eşyası";
            case "saucer": return "Tabak";
            case "computer": return "Bilgisayar";
            case "laptop": return "Dizüstü Bilgisayar";
            case "keyboard": return "Klavye";
            case "coffee": return "Kahve";
            case "mobile phone": return "Telefon";
            case "phone": return "Telefon";
            case "notebook": return "Defter";
            case "paper": return "Kağıt";
            case "pen": return "Kalem";
            case "writing": return "Yazı";
            case "shelf": return "Raf";
            case "lipstick": return "Ruj";
            case "jacket": return "Ceket";
            case "leather": return "Deri";
            case "chair": return "Sandalye";
            case "building": return "Bina";
            case "wall": return "Duvar";
            case "bench": return "Bank";
            case "pattern": return "Desen";
            case "monochrome": return "Tek Renk";
            case "fork": return "Çatal";
            case "cutlery": return "Çatal Bıçak";
            case "wing": return "Kanat";
            case "beach": return "Sahil";
            case "rock": return "Kaya";
            case "sky": return "Gökyüzü";
            case "vacation": return "Tatil";
            case "leisure": return "Dinlenme";
            case "mountain": return "Dağ";
            case "cliff": return "Uçurum";
            case "bird": return "Kuş";
            case "water": return "Su";
            case "waterfall": return "Şelale";
            case "river": return "Nehir";
            case "lake": return "Göl";
            case "sea": return "Deniz";
            case "ocean": return "Okyanus";
            case "sand": return "Kum";
            case "forest": return "Orman";
            case "field": return "Tarla";
            case "prairie": return "Çayır";
            case "plant": return "Bitki";
            case "flower": return "Çiçek";
            case "road": return "Yol";
            case "asphalt": return "Asfalt";
            case "grass": return "Çimen";
            case "tree": return "Ağaç";
            case "branch": return "Dal";
            case "twig": return "İnce Dal";
            case "insect": return "Böcek";
            case "vehicle": return "Araç";
            case "wheel": return "Tekerlek";
            case "tire": return "Lastik";
            case "car": return "Araba";
            case "windshield": return "Ön Cam";
            case "cat": return "Kedi";
            case "dog": return "Köpek";
            case "fur": return "Kürk";
            case "snout": return "Burun";
            case "textile": return "Tekstil";
            case "toy": return "Oyuncak";
            case "shoe": return "Ayakkabı";
            case "shoes": return "Ayakkabılar";
            case "foot": return "Ayak";
            case "flesh": return "Ten Rengi";
            case "curtain": return "Perde";
            case "sneakers": return "Spor Ayakkabı";
            case "footwear": return "Ayakkabı";
            case "light": return "Işık";
            case "lights": return "Işıklar";
            case "blur": return "Bulanıklık";
            case "bokeh": return "Bokeh";
            case "color": return "Renk";
            case "texture": return "Doku";
            case "surface": return "Yüzey";
            case "droplet": return "Su Damlası";
            case "water drop": return "Su Damlası";
            case "macro": return "Makro";
            case "architecture": return "Mimari";
            case "city": return "Şehir";
            case "street": return "Sokak";
            case "person": return "İnsan";
            case "people": return "İnsanlar";
            case "portrait": return "Portre";
            default:
                return label;
        }
    }
}
