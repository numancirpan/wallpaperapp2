package com.example.wallpaperapp2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ImageView imageProfileCover;
    private ImageView imageProfilePhoto;
    private TextView txtProfileDisplayName;
    private TextView txtProfileEmail;
    private TextView txtFavoriteCount;
    private TextView txtPostCount;
    private TextView txtCompletionPercent;
    private TextView txtBioCounter;
    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editBio;
    private MaterialButton btnSaveProfile;
    private MaterialButton btnChangePassword;
    private MaterialButton btnDeleteAccount;
    private MaterialButton btnChooseCover;
    private MaterialButton btnChooseProfilePhoto;
    private RecyclerView recyclerBlogPosts;
    private MaterialCardView cardBlogEmptyState;

    private UserProfile currentProfile = new UserProfile();
    private BlogPostAdapter blogPostAdapter;
    private ListenerRegistration profileListener;
    private ListenerRegistration postsListener;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private int currentPostCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        registerImagePicker();
        bindViews(view);
        setupBioCounter();
        setupBlogList();
        registerActions();
        startListeners();
        return view;
    }

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            uploadProfilePhoto(uri);
        });
    }

    private void bindViews(View view) {
        imageProfileCover = view.findViewById(R.id.imageProfileCover);
        imageProfilePhoto = view.findViewById(R.id.imageProfilePhoto);
        txtProfileDisplayName = view.findViewById(R.id.txtProfileDisplayName);
        txtProfileEmail = view.findViewById(R.id.txtProfileEmail);
        txtFavoriteCount = view.findViewById(R.id.txtFavoriteCount);
        txtPostCount = view.findViewById(R.id.txtPostCount);
        txtCompletionPercent = view.findViewById(R.id.txtCompletionPercent);
        txtBioCounter = view.findViewById(R.id.txtBioCounter);
        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        editBio = view.findViewById(R.id.editBio);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnChooseCover = view.findViewById(R.id.btnChooseCover);
        btnChooseProfilePhoto = view.findViewById(R.id.btnChooseProfilePhoto);
        recyclerBlogPosts = view.findViewById(R.id.recyclerBlogPosts);
        cardBlogEmptyState = view.findViewById(R.id.cardBlogEmptyState);
    }

    private void setupBioCounter() {
        updateBioCounter(getText(editBio).length());
        editBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateBioCounter(s == null ? 0 : s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupBlogList() {
        blogPostAdapter = new BlogPostAdapter(new ArrayList<>(), new BlogPostAdapter.OnPostActionListener() {
            @Override
            public void onEdit(BlogPost post) {
                showEditPostDialog(post);
            }

            @Override
            public void onDelete(BlogPost post) {
                UserProfileStore.deleteBlogPost(post.id, (success, errorMessage) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            showMessage(getString(R.string.post_deleted));
                        } else {
                            showMessage(errorMessage == null ? getString(R.string.post_failed, "Unknown error") : errorMessage);
                        }
                    });
                });
            }
        });
        recyclerBlogPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBlogPosts.setAdapter(blogPostAdapter);
    }

    private void registerActions() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        btnChooseCover.setOnClickListener(v -> chooseCoverFromFavorites());
        btnChooseProfilePhoto.setOnClickListener(v -> showProfilePhotoOptions());
    }

    private void startListeners() {
        stopListeners();
        profileListener = UserProfileStore.listenProfile(profile -> {
            currentProfile = profile == null ? new UserProfile() : profile;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(this::renderProfile);
        });
        postsListener = UserProfileStore.listenBlogPosts(posts -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> renderPosts(posts));
        });
    }

    private void renderProfile() {
        String email = UserProfileStore.currentEmail();
        txtProfileDisplayName.setText(currentProfile.getDisplayName(email));
        txtProfileEmail.setText(email);

        setTextIfDifferent(editFirstName, currentProfile.firstName);
        setTextIfDifferent(editLastName, currentProfile.lastName);
        setTextIfDifferent(editBio, currentProfile.bio);
        updateBioCounter(getText(editBio).length());
        renderStats();

        if (currentProfile.profilePhotoUrl != null && !currentProfile.profilePhotoUrl.trim().isEmpty()) {
            Glide.with(this).load(currentProfile.profilePhotoUrl).centerCrop().into(imageProfilePhoto);
        } else {
            imageProfilePhoto.setImageResource(R.drawable.logo_placeholder);
        }

        if (currentProfile.coverImageUrl != null && !currentProfile.coverImageUrl.trim().isEmpty()) {
            Glide.with(this).load(currentProfile.coverImageUrl).centerCrop().into(imageProfileCover);
        } else {
            imageProfileCover.setImageResource(R.drawable.logo_placeholder);
        }
    }

    private void renderPosts(List<BlogPost> posts) {
        List<BlogPost> safePosts = posts == null ? new ArrayList<>() : posts;
        currentPostCount = safePosts.size();
        blogPostAdapter.updateList(safePosts);
        cardBlogEmptyState.setVisibility(safePosts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerBlogPosts.setVisibility(safePosts.isEmpty() ? View.GONE : View.VISIBLE);
        renderStats();
    }

    private void renderStats() {
        int favoriteCount = WallpaperRepository.getFavoriteWallpapers().size();
        txtFavoriteCount.setText(String.valueOf(favoriteCount));
        txtPostCount.setText(String.valueOf(currentPostCount));
        txtCompletionPercent.setText(String.format(Locale.getDefault(), "%d%%", calculateProfileCompletion()));
    }

    private int calculateProfileCompletion() {
        int completed = 0;
        int total = 5;
        if (!safe(currentProfile.firstName).isEmpty()) completed++;
        if (!safe(currentProfile.lastName).isEmpty()) completed++;
        if (!safe(currentProfile.bio).isEmpty()) completed++;
        if (!safe(currentProfile.profilePhotoUrl).isEmpty()) completed++;
        if (!safe(currentProfile.coverImageUrl).isEmpty()) completed++;
        return Math.round((completed * 100f) / total);
    }

    private void saveProfile() {
        String bio = getText(editBio);
        if (bio.length() > UserProfileStore.MAX_BIO_LENGTH) {
            editBio.setError(getString(R.string.bio_too_long, UserProfileStore.MAX_BIO_LENGTH));
            return;
        }

        currentProfile.firstName = limit(getText(editFirstName), UserProfileStore.MAX_NAME_LENGTH);
        currentProfile.lastName = limit(getText(editLastName), UserProfileStore.MAX_NAME_LENGTH);
        currentProfile.bio = limit(bio, UserProfileStore.MAX_BIO_LENGTH);

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText(R.string.saving);
        UserProfileStore.saveProfile(currentProfile, (success, errorMessage) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                btnSaveProfile.setEnabled(true);
                btnSaveProfile.setText(R.string.save_profile);
                if (success) {
                    showMessage(getString(R.string.profile_saved));
                } else {
                    showMessage(getString(R.string.profile_save_failed, errorMessage == null ? "Unknown error" : errorMessage));
                }
            });
        });
    }

    private void showEditPostDialog(BlogPost post) {
        if (post == null || !isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_repost, null, false);
        ImageView imagePreview = dialogView.findViewById(R.id.imageRepostPreview);
        TextInputEditText editComment = dialogView.findViewById(R.id.editRepostComment);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelRepost);
        MaterialButton btnShare = dialogView.findViewById(R.id.btnShareRepost);
        TextView title = dialogView.findViewById(R.id.txtRepostDialogTitle);
        TextView counter = dialogView.findViewById(R.id.txtRepostCounter);

        title.setText(R.string.edit_post_title);
        btnShare.setText(R.string.update_post);
        editComment.setText(post.comment);
        RepostDialogHelper.attachCommentCounter(editComment, counter);

        Glide.with(this).load(post.imageUrl).centerCrop().into(imagePreview);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        dialog.setOnShowListener(d -> RepostDialogHelper.animateDialogIn(dialogView));

        btnCancel.setOnClickListener(v -> RepostDialogHelper.dismissWithAnimation(dialog, dialogView));
        btnShare.setOnClickListener(v -> {
            String comment = getText(editComment);
            if (comment.isEmpty()) {
                editComment.setError(getString(R.string.comment_required));
                return;
            }
            if (comment.length() > UserProfileStore.MAX_COMMENT_LENGTH) {
                editComment.setError(getString(R.string.comment_too_long, UserProfileStore.MAX_COMMENT_LENGTH));
                return;
            }
            btnShare.setEnabled(false);
            btnCancel.setEnabled(false);
            btnShare.setText(R.string.post_saving);
            RepostDialogHelper.dismissWithAnimation(dialog, dialogView);
            showMessage(getString(R.string.post_saving));
            UserProfileStore.updateBlogPost(post.id, comment, (success, errorMessage) -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        showMessage(getString(R.string.post_updated));
                    } else {
                        showMessage(getString(R.string.post_failed, errorMessage == null ? "Unknown error" : errorMessage));
                    }
                });
            });
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showProfilePhotoOptions() {
        String[] options = new String[]{getString(R.string.photo_from_phone), getString(R.string.photo_from_favorites)};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.choose_profile_photo)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        imagePickerLauncher.launch("image/*");
                    } else {
                        chooseProfilePhotoFromFavorites();
                    }
                })
                .show();
    }

    private void uploadProfilePhoto(Uri uri) {
        showMessage(getString(R.string.uploading_photo));
        UserProfileStore.uploadProfilePhoto(requireContext(), uri, (success, errorMessage) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (success) showMessage(getString(R.string.photo_updated));
                else showMessage(getString(R.string.photo_upload_failed, errorMessage == null ? "Unknown error" : errorMessage));
            });
        });
    }

    private void chooseProfilePhotoFromFavorites() {
        showWallpaperPicker(R.string.profile_photo, selected ->
                UserProfileStore.updateProfilePhotoFromWallpaper(selected, (success, errorMessage) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) showMessage(getString(R.string.photo_updated));
                        else showMessage(getString(R.string.profile_save_failed, errorMessage == null ? "Unknown error" : errorMessage));
                    });
                })
        );
    }

    private void chooseCoverFromFavorites() {
        showWallpaperPicker(R.string.cover_photo, selected ->
                UserProfileStore.updateCoverFromWallpaper(selected, (success, errorMessage) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) showMessage(getString(R.string.cover_updated));
                        else showMessage(getString(R.string.profile_save_failed, errorMessage == null ? "Unknown error" : errorMessage));
                    });
                })
        );
    }

    private void showWallpaperPicker(int titleRes, SelectableWallpaperAdapter.OnWallpaperSelectedListener listener) {
        List<Wallpaper> favorites = WallpaperRepository.getFavoriteWallpapers();
        if (favorites.isEmpty()) {
            showMessage(getString(R.string.no_favorites_for_cover));
            return;
        }

        View pickerView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wallpaper_picker, null, false);
        RecyclerView recyclerPicker = pickerView.findViewById(R.id.recyclerWallpaperPicker);
        recyclerPicker.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setView(pickerView)
                .setNegativeButton(R.string.cancel, null)
                .create();

        recyclerPicker.setAdapter(new SelectableWallpaperAdapter(favorites, wallpaper -> {
            listener.onSelected(wallpaper);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null, false);
        TextInputEditText oldPassword = dialogView.findViewById(R.id.editOldPassword);
        TextInputEditText newPassword = dialogView.findViewById(R.id.editNewPassword);
        TextInputEditText confirmPassword = dialogView.findViewById(R.id.editConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_password)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset_password, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPass = getText(oldPassword);
            String newPass = getText(newPassword);
            String confirmPass = getText(confirmPassword);

            if (oldPass.length() < 6) {
                oldPassword.setError(getString(R.string.weak_password));
                return;
            }
            if (newPass.length() < 6) {
                newPassword.setError(getString(R.string.weak_password));
                return;
            }
            if (oldPass.equals(newPass)) {
                newPassword.setError(getString(R.string.new_password_same_as_old));
                return;
            }
            if (!newPass.equals(confirmPass)) {
                confirmPassword.setError(getString(R.string.passwords_do_not_match));
                return;
            }

            changePassword(oldPass, newPass, dialog);
        }));

        dialog.show();
    }

    private void changePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user == null ? "" : user.getEmail();
        if (user == null || email == null || email.trim().isEmpty()) return;

        user.reauthenticate(EmailAuthProvider.getCredential(email, oldPassword))
                .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                        .addOnSuccessListener(update -> {
                            showMessage(getString(R.string.password_changed));
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> showMessage(getString(R.string.password_change_failed, e.getMessage()))))
                .addOnFailureListener(e -> showMessage(getString(R.string.password_change_failed, e.getMessage())));
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_account_confirm_title)
                .setMessage(R.string.delete_account_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        btnDeleteAccount.setEnabled(false);
        user.delete()
                .addOnSuccessListener(unused -> {
                    showMessage(getString(R.string.account_deleted));
                    startActivity(new Intent(requireContext(), AuthActivity.class));
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    btnDeleteAccount.setEnabled(true);
                    showMessage(getString(R.string.delete_account_failed, e.getMessage()));
                });
    }

    private void setTextIfDifferent(TextInputEditText editText, String value) {
        String next = value == null ? "" : value;
        if (editText.getText() == null || !editText.getText().toString().equals(next)) {
            editText.setText(next);
        }
    }

    private void updateBioCounter(int length) {
        if (txtBioCounter != null) {
            txtBioCounter.setText(String.format(Locale.getDefault(), "%d / %d", length, UserProfileStore.MAX_BIO_LENGTH));
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String limit(String value, int maxLength) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void stopListeners() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListeners();
    }
}
