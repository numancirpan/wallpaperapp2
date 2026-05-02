package com.example.wallpaperapp2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RepostDialogHelper {

    public static void show(Context context, View anchorView, Wallpaper wallpaper) {
        if (context == null || anchorView == null || wallpaper == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_repost, null, false);
        TextInputEditText editComment = dialogView.findViewById(R.id.editRepostComment);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.write_comment)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.share, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String comment = editComment.getText() == null ? "" : editComment.getText().toString().trim();
            if (comment.isEmpty()) {
                editComment.setError(context.getString(R.string.comment_required));
                return;
            }

            UserProfileStore.addBlogPost(wallpaper, comment, (success, errorMessage) -> anchorView.post(() -> {
                if (success) {
                    dialog.dismiss();
                    Snackbar.make(anchorView, R.string.post_saved, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(anchorView,
                            context.getString(R.string.post_failed, errorMessage == null ? "Unknown error" : errorMessage),
                            Snackbar.LENGTH_SHORT).show();
                }
            }));
        }));

        dialog.show();
    }
}
