package com.example.wallpaperapp2;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RepostDialogHelper {

    public static void attachCommentCounter(TextInputEditText editComment, TextView counter) {
        if (editComment == null || counter == null) return;

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s == null ? 0 : s.length();
                counter.setText(length + " / " + UserProfileStore.MAX_COMMENT_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        editComment.addTextChangedListener(watcher);
        Editable initialText = editComment.getText();
        int initialLength = initialText == null ? 0 : initialText.length();
        counter.setText(initialLength + " / " + UserProfileStore.MAX_COMMENT_LENGTH);
    }

    public static void show(Context context, View anchorView, Wallpaper wallpaper) {
        if (context == null || anchorView == null || wallpaper == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_repost, null, false);
        ImageView imageRepostPreview = dialogView.findViewById(R.id.imageRepostPreview);
        TextInputEditText editComment = dialogView.findViewById(R.id.editRepostComment);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelRepost);
        MaterialButton btnShare = dialogView.findViewById(R.id.btnShareRepost);
        TextView counter = dialogView.findViewById(R.id.txtRepostCounter);

        attachCommentCounter(editComment, counter);

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

            dialog.dismiss();
            Snackbar.make(anchorView, R.string.post_saved, Snackbar.LENGTH_SHORT).show();

            UserProfileStore.addBlogPost(wallpaper, comment, (success, errorMessage) -> anchorView.post(() -> {
                if (!success) {
                    Snackbar.make(anchorView,
                            context.getString(R.string.post_failed, errorMessage == null ? "Unknown error" : errorMessage),
                            Snackbar.LENGTH_LONG).show();
                }
            }));
        });

        dialog.show();
    }
}
