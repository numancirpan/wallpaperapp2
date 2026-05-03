# Firebase Setup

1. Firebase Console'dan Android uygulamasini `com.example.wallpaperapp2` package id ile ekle.
2. Indirdigin `google-services.json` dosyasini su konuma koy:
   - `app/google-services.json`
3. `local.properties` icine su satirlari ekle (varsa guncelle):

```
GEMINI_API_KEY=YOUR_GEMINI_KEY
WALLPAPER_API_URL=https://picsum.photos/v2/list?page=1&limit=60
```

4. Android Studio'da Gradle Sync yap.
5. Sonrasinda app icinde Firebase Auth ve Firestore otomatik aktif olur.
