package com.example.wallpaperapp2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RepostDialogHelper {

    public static void show(Context context, View anchorView, Wallpaper wallpaper) {
        if (context == null || anchorView == null || wallpaper == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_repost, null, false);
        ImageView imageRepostPreview = dialogView.findViewById(R.id.imageRepostPreview);
        TextInputEditText editComment = dialogView.findViewById(R.id.editRepostComment);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelRepost);
        MaterialButton btnShare = dialogView.findViewById(R.id.btnShareRepost);

        if (wallpaper.hasRemoteImage()) {
            Glide.with(context).load(wallpaper.imageUrl).centerCrop().into(imageRepostPreview);
        } else {
            imageRepostPreview.setImageResource(wallpaper.imageRes);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnShare.setOnClickListener(v -> {
            String comment = editComment.getText() == null ? "" : editComment.getText().toString().trim();
            if (comment.isEmpty()) {
                editComment.setError(context.getString(R.string.comment_required));
                return;
            }

            btnShare.setEnabled(false);
            UserProfileStore.addBlogPost(wallpaper, comment, (success, errorMessage) -> anchorView.post(() -> {
                btnShare.setEnabled(true);
                if (success) {
                    dialog.dismiss();
                    Snackbar.make(anchorView, R.string.post_saved, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(anchorView,
                            context.getString(R.string.post_failed, errorMessage == null ? "Unknown error" : errorMessage),
                            Snackbar.LENGTH_SHORT).show();
                }
            }));
        });

        dialog.show();
    }
}
