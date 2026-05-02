package com.example.wallpaperapp2;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ProfileFragment extends Fragment {

    private ImageView imageProfileCover;
    private ImageView imageProfilePhoto;
    private TextView txtProfileDisplayName;
    private TextView txtProfileEmail;
    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editBio;
    private MaterialButton btnSaveProfile;
    private MaterialButton btnChangePassword;
    private MaterialButton btnChooseCover;
    private MaterialButton btnChooseProfilePhoto;
    private RecyclerView recyclerBlogPosts;
    private MaterialCardView cardBlogEmptyState;

    private UserProfile currentProfile = new UserProfile();
    private BlogPostAdapter blogPostAdapter;
    private ListenerRegistration profileListener;
    private ListenerRegistration postsListener;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        registerImagePicker();
        bindViews(view);
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
        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        editBio = view.findViewById(R.id.editBio);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnChooseCover = view.findViewById(R.id.btnChooseCover);
        btnChooseProfilePhoto = view.findViewById(R.id.btnChooseProfilePhoto);
        recyclerBlogPosts = view.findViewById(R.id.recyclerBlogPosts);
        cardBlogEmptyState = view.findViewById(R.id.cardBlogEmptyState);
    }

    private void setupBlogList() {
        blogPostAdapter = new BlogPostAdapter(new ArrayList<>(), post ->
                UserProfileStore.deleteBlogPost(post.id, (success, errorMessage) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            showMessage(getString(R.string.post_deleted));
                        } else {
                            showMessage(errorMessage == null ? getString(R.string.post_failed, "Unknown error") : errorMessage);
                        }
                    });
                })
        );
        recyclerBlogPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBlogPosts.setAdapter(blogPostAdapter);
    }

    private void registerActions() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
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
        blogPostAdapter.updateList(safePosts);
        cardBlogEmptyState.setVisibility(safePosts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerBlogPosts.setVisibility(safePosts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void saveProfile() {
        currentProfile.firstName = getText(editFirstName);
        currentProfile.lastName = getText(editLastName);
        currentProfile.bio = getText(editBio);

        btnSaveProfile.setEnabled(false);
        UserProfileStore.saveProfile(currentProfile, (success, errorMessage) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                btnSaveProfile.setEnabled(true);
                if (success) {
                    showMessage(getString(R.string.profile_saved));
                } else {
                    showMessage(getString(R.string.profile_save_failed, errorMessage == null ? "Unknown error" : errorMessage));
                }
            });
        });
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
        UserProfileStore.uploadProfilePhoto(uri, (success, errorMessage) -> {
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

    private void setTextIfDifferent(TextInputEditText editText, String value) {
        String next = value == null ? "" : value;
        if (editText.getText() == null || !editText.getText().toString().equals(next)) {
            editText.setText(next);
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
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
