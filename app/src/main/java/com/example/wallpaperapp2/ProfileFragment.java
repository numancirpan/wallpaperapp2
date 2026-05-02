package com.example.wallpaperapp2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
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
    private TextInputEditText editProfilePhotoUrl;
    private MaterialButton btnSaveProfile;
    private MaterialButton btnSendResetEmail;
    private MaterialButton btnChooseCover;
    private RecyclerView recyclerBlogPosts;
    private MaterialCardView cardBlogEmptyState;

    private UserProfile currentProfile = new UserProfile();
    private BlogPostAdapter blogPostAdapter;
    private ListenerRegistration profileListener;
    private ListenerRegistration postsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        bindViews(view);
        setupBlogList();
        registerActions();
        startListeners();
        return view;
    }

    private void bindViews(View view) {
        imageProfileCover = view.findViewById(R.id.imageProfileCover);
        imageProfilePhoto = view.findViewById(R.id.imageProfilePhoto);
        txtProfileDisplayName = view.findViewById(R.id.txtProfileDisplayName);
        txtProfileEmail = view.findViewById(R.id.txtProfileEmail);
        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        editBio = view.findViewById(R.id.editBio);
        editProfilePhotoUrl = view.findViewById(R.id.editProfilePhotoUrl);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnSendResetEmail = view.findViewById(R.id.btnSendResetEmail);
        btnChooseCover = view.findViewById(R.id.btnChooseCover);
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
        btnSendResetEmail.setOnClickListener(v -> sendPasswordReset());
        btnChooseCover.setOnClickListener(v -> chooseCoverFromFavorites());
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
        setTextIfDifferent(editProfilePhotoUrl, currentProfile.profilePhotoUrl);

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
        currentProfile.profilePhotoUrl = getText(editProfilePhotoUrl);

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

    private void sendPasswordReset() {
        String email = UserProfileStore.currentEmail();
        if (email.trim().isEmpty()) return;
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> showMessage(getString(R.string.password_reset_sent)))
                .addOnFailureListener(e -> showMessage(getString(R.string.password_reset_failed, e.getMessage())));
    }

    private void chooseCoverFromFavorites() {
        List<Wallpaper> favorites = WallpaperRepository.getFavoriteWallpapers();
        if (favorites.isEmpty()) {
            showMessage(getString(R.string.no_favorites_for_cover));
            return;
        }

        String[] names = new String[favorites.size()];
        for (int i = 0; i < favorites.size(); i++) {
            Wallpaper wallpaper = favorites.get(i);
            String category = CategoryDisplayMapper.toDisplayName(requireContext(), wallpaper.aiCategory);
            names[i] = wallpaper.title + " - " + category;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cover_photo)
                .setItems(names, (dialog, which) -> {
                    Wallpaper selected = favorites.get(which);
                    UserProfileStore.updateCoverFromWallpaper(selected, (success, errorMessage) -> {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (success) showMessage(getString(R.string.cover_updated));
                            else showMessage(getString(R.string.profile_save_failed, errorMessage == null ? "Unknown error" : errorMessage));
                        });
                    });
                })
                .show();
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
