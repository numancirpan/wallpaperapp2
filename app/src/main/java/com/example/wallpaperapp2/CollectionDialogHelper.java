package com.example.wallpaperapp2;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class CollectionDialogHelper {

    public static void show(Context context, View anchor, Wallpaper wallpaper) {
        if (context == null || wallpaper == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(context);
        LinearLayout content = buildSheetContent(context);
        LinearLayout listContainer = content.findViewWithTag("collections_list");
        renderCollectionRows(context, anchor, wallpaper, sheet, listContainer, UserProfileStore.getCachedCollections());

        sheet.setContentView(content);
        sheet.show();

        UserProfileStore.fetchCollections(collections ->
                listContainer.post(() -> renderCollectionRows(context, anchor, wallpaper, sheet, listContainer, collections))
        );
    }

    private static LinearLayout buildSheetContent(Context context) {
        int padding = dp(context, 20);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, 12, padding, padding);

        TextView handle = new TextView(context);
        handle.setText("----");
        handle.setTextColor(0xFFB7A8CC);
        handle.setGravity(android.view.Gravity.CENTER);
        root.addView(handle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(R.string.add_to_collection);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 10, 0, 4);
        root.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText(R.string.choose_collection_description);
        subtitle.setAlpha(0.72f);
        subtitle.setTextSize(14);
        root.addView(subtitle);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setTag("collections_list");
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list);
        root.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private static void renderCollectionRows(
            Context context,
            View anchor,
            Wallpaper wallpaper,
            BottomSheetDialog sheet,
            LinearLayout listContainer,
            List<WallpaperCollection> collections
    ) {
        listContainer.removeAllViews();
        List<WallpaperCollection> safeCollections = collections == null ? new ArrayList<>() : collections;

        if (safeCollections.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(R.string.no_collections_yet_description);
            empty.setAlpha(0.72f);
            empty.setPadding(0, dp(context, 14), 0, dp(context, 8));
            listContainer.addView(empty);
        }

        for (WallpaperCollection collection : safeCollections) {
            if (collection.name == null || collection.name.trim().isEmpty()) continue;
            boolean containsWallpaper = collectionContains(collection, wallpaper.id);
            listContainer.addView(collectionRow(context, collection, containsWallpaper, v -> {
                if (containsWallpaper) return;
                UserProfileStore.addWallpaperToCollection(collection.id, wallpaper, (success, errorMessage) -> {
                    if (success) {
                        UserProfileStore.notifyCollectionsUiChanged();
                    }
                    showResult(context, anchor, success, errorMessage);
                });
                sheet.dismiss();
            }, v -> {
                UserProfileStore.removeWallpaperFromCollection(collection.id, wallpaper.id, (success, errorMessage) -> {
                    if (success) {
                        UserProfileStore.notifyCollectionsUiChanged();
                    }
                    showRemoveResult(context, anchor, success, errorMessage);
                });
                sheet.dismiss();
            }));
        }

        MaterialButton newButton = new MaterialButton(context);
        newButton.setText(R.string.new_collection);
        newButton.setIconResource(android.R.drawable.ic_menu_add);
        newButton.setCornerRadius(dp(context, 22));
        newButton.setOnClickListener(v -> {
            sheet.dismiss();
            showNewCollectionDialog(context, anchor, wallpaper);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 52)
        );
        params.setMargins(0, dp(context, 12), 0, 0);
        listContainer.addView(newButton, params);
    }

    private static View collectionRow(
            Context context,
            WallpaperCollection collection,
            boolean containsWallpaper,
            View.OnClickListener addListener,
            View.OnClickListener removeListener
    ) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        row.setBackgroundColor(containsWallpaper ? 0x22C7B2FF : 0x10C7B2FF);
        row.setOnClickListener(addListener);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(context);
        name.setText(collection.name);
        name.setTextSize(17);
        name.setTypeface(null, Typeface.BOLD);
        textColumn.addView(name);

        int count = collection.wallpapers == null ? 0 : collection.wallpapers.size();
        TextView meta = new TextView(context);
        meta.setText(containsWallpaper
                ? context.getString(R.string.in_this_collection_hint)
                : context.getResources().getQuantityString(R.plurals.collection_photo_count, count, count));
        meta.setAlpha(0.68f);
        meta.setTextSize(13);
        textColumn.addView(meta);

        if (containsWallpaper) {
            MaterialButton remove = new MaterialButton(context);
            remove.setText(R.string.remove_from_collection);
            remove.setIconResource(android.R.drawable.ic_menu_delete);
            remove.setTextSize(12);
            remove.setMinWidth(0);
            remove.setCornerRadius(dp(context, 18));
            remove.setTextColor(0xFFFFFFFF);
            remove.setIconTint(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            remove.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6E4BA8));
            remove.setOnClickListener(removeListener);
            row.addView(remove, new LinearLayout.LayoutParams(dp(context, 116), dp(context, 40)));
        } else {
            TextView arrow = new TextView(context);
            arrow.setText(">");
            arrow.setTextSize(22);
            arrow.setTextColor(0xFF6E4BA8);
            row.addView(arrow);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(context, 10), 0, 0);
        row.setLayoutParams(params);
        return row;
    }

    private static void showNewCollectionDialog(Context context, View anchor, Wallpaper wallpaper) {
        TextInputLayout inputLayout = new TextInputLayout(context);
        inputLayout.setHint(context.getString(R.string.collection_name));
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        int padding = dp(context, 20);
        inputLayout.setPadding(padding, 8, padding, 0);

        TextInputEditText input = new TextInputEditText(context);
        input.setSingleLine(true);
        inputLayout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.new_collection)
                .setMessage(R.string.add_to_collection_description)
                .setView(inputLayout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(context.getString(R.string.collection_name_required));
                return;
            }

            UserProfileStore.addWallpaperToCollectionByName(name, wallpaper, (success, errorMessage) ->
            {
                if (success) {
                    UserProfileStore.notifyCollectionsUiChanged();
                }
                showResult(context, anchor, success, errorMessage);
            }
            );
            dialog.dismiss();
        }));

        dialog.show();
    }

    private static void showResult(Context context, View anchor, boolean success, String errorMessage) {
        if (anchor == null) return;
        String message = success
                ? context.getString(R.string.collection_saved)
                : context.getString(R.string.collection_save_failed, errorMessage == null ? "Unknown error" : errorMessage);
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show();
    }

    private static void showRemoveResult(Context context, View anchor, boolean success, String errorMessage) {
        if (anchor == null) return;
        String message = success
                ? context.getString(R.string.collection_removed)
                : context.getString(R.string.collection_save_failed, errorMessage == null ? "Unknown error" : errorMessage);
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show();
    }

    private static boolean collectionContains(WallpaperCollection collection, int wallpaperId) {
        if (collection.wallpapers == null) return false;
        for (Wallpaper wallpaper : collection.wallpapers) {
            if (wallpaper != null && wallpaper.id == wallpaperId) return true;
        }
        return false;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
