package com.example.wallpaperapp2;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class RepostDialogHelper {

    private static final long DIALOG_ANIMATION_DURATION_MS = 180L;

    public static void show(Context context, View anchorView, Wallpaper wallpaper) {
        if (context == null || anchorView == null || wallpaper == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_repost, null, false);
        ImageView imageRepostPreview = dialogView.findViewById(R.id.imageRepostPreview);
        TextInputEditText editComment = dialogView.findViewById(R.id.editRepostComment);
        TextView txtCounter = dialogView.findViewById(R.id.txtRepostCounter);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelRepost);
        MaterialButton btnShare = dialogView.findViewById(R.id.btnShareRepost);

        attachCommentCounter(editComment, txtCounter);

        if (wallpaper.hasRemoteImage()) {
            Glide.with(context).load(wallpaper.imageUrl).centerCrop().into(imageRepostPreview);
        } else {
            imageRepostPreview.setImageResource(wallpaper.imageRes);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialog.setOnShowListener(d -> animateDialogIn(dialogView));

        btnCancel.setOnClickListener(v -> dismissWithAnimation(dialog, dialogView));
        btnShare.setOnClickListener(v -> {
            String comment = editComment.getText() == null ? "" : editComment.getText().toString().trim();
            if (comment.isEmpty()) {
                editComment.setError(context.getString(R.string.comment_required));
                return;
            }
            if (comment.length() > UserProfileStore.MAX_COMMENT_LENGTH) {
                editComment.setError(context.getString(R.string.comment_too_long, UserProfileStore.MAX_COMMENT_LENGTH));
                return;
            }

            btnShare.setEnabled(false);
            btnCancel.setEnabled(false);
            btnShare.setText(R.string.posting);
            dismissWithAnimation(dialog, dialogView);
            Snackbar.make(anchorView, R.string.post_saving, Snackbar.LENGTH_SHORT).show();

            UserProfileStore.addBlogPost(wallpaper, comment, (success, errorMessage) -> anchorView.post(() -> {
                if (success) {
                    Snackbar.make(anchorView, R.string.post_saved_done, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(anchorView,
                            context.getString(R.string.post_failed, errorMessage == null ? "Unknown error" : errorMessage),
                            Snackbar.LENGTH_LONG).show();
                }
            }));
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    public static void attachCommentCounter(TextInputEditText editComment, TextView txtCounter) {
        if (editComment == null || txtCounter == null) return;
        updateCounter(txtCounter, editComment.getText() == null ? 0 : editComment.getText().length());
        editComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCounter(txtCounter, s == null ? 0 : s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public static void animateDialogIn(View dialogView) {
        if (dialogView == null) return;
        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.96f);
        dialogView.setScaleY(0.96f);
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(DIALOG_ANIMATION_DURATION_MS)
                .start();
    }

    public static void dismissWithAnimation(AlertDialog dialog, View dialogView) {
        if (dialog == null || dialogView == null) return;
        dialogView.animate()
                .alpha(0f)
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(DIALOG_ANIMATION_DURATION_MS)
                .withEndAction(dialog::dismiss)
                .start();
    }

    private static void updateCounter(TextView txtCounter, int length) {
        txtCounter.setText(String.format(Locale.getDefault(), "%d / %d", length, UserProfileStore.MAX_COMMENT_LENGTH));
    }
}
