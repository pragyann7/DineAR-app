package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ps.dinear.auth.AuthActivity;

import android.widget.PopupMenu;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private int tempSelectedAvatar = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        ImageView ivProfile = view.findViewById(R.id.ivProfileImage);
        
        if (ivProfile != null && getContext() != null) {
            ivProfile.setImageResource(SharedPrefManager.getUserAvatar(getContext()));
        }

        updateProfileInfo(tvName, tvEmail);

        View nestedScroll = view.findViewById(R.id.nestedScrollViewProfile);
        if (nestedScroll != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(nestedScroll, (v, insets) -> {
                androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
                float density = v.getResources().getDisplayMetrics().density;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), (int) (40 * density) + navBars.bottom);
                return insets;
            });
        }
        
        if (ivProfile != null) {
            View.OnClickListener avatarTrigger = v -> {
                if (SharedPrefManager.isGuest(requireContext())) {
                    Toast.makeText(getContext(), "Please login to change avatar", Toast.LENGTH_SHORT).show();
                    return;
                }
                showChangeAvatarSheet(ivProfile);
            };
            ivProfile.setOnClickListener(avatarTrigger);
            View btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
            if (btnEditAvatar != null) btnEditAvatar.setOnClickListener(avatarTrigger);
        }

        View btnPersonal = view.findViewById(R.id.btnPersonalInfo);
        if (btnPersonal != null) {
            btnPersonal.setOnClickListener(v -> {
                if (SharedPrefManager.isGuest(requireContext())) {
                    Toast.makeText(getContext(), "Please login to edit profile", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(getContext(), EditProfileActivity.class));
                }
            });
        }
        
        View btnOrders = view.findViewById(R.id.btnProfileOrders);
        if (btnOrders != null) {
            btnOrders.setOnClickListener(v -> {
                if (SharedPrefManager.isGuest(requireContext())) {
                    Toast.makeText(getContext(), "Please login to view orders", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(getContext(), OrdersActivity.class));
                }
            });
        }

        // Removed Saved Addresses option as requested
        View btnAddresses = view.findViewById(R.id.btnProfileAddresses);
        if (btnAddresses != null) btnAddresses.setVisibility(View.GONE);
        
        View btnChangePassword = view.findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                if (SharedPrefManager.isGuest(requireContext())) {
                    Toast.makeText(getContext(), "Please login to change password", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(getContext(), ChangePasswordActivity.class));
                }
            });
        }

        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> startActivity(new Intent(getContext(), ServerConfigActivity.class)));

        View btnMore = view.findViewById(R.id.btnMore);
        if (btnMore != null) {
            btnMore.setOnClickListener(this::showMainOverflowMenu);
        }

        View btnLogout = view.findViewById(R.id.btnProfileLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (!isAdded()) return;
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_alert, null);
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();
                if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v1 -> dialog.dismiss());
                dialogView.findViewById(R.id.btnDialogConfirm).setOnClickListener(v1 -> {
                    dialog.dismiss();
                    performLogout();
                });
                dialog.show();
            });
        }

        View btnRegister = view.findViewById(R.id.btnProfileRegister);
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Restaurant Registration")
                    .setMessage("To register your restaurant, please contact our support team at business@dinear.com or call +977-9800000000.")
                    .setPositiveButton("Close", null)
                    .show();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && isAdded()) {
            updateProfileInfo(getView().findViewById(R.id.tvProfileName), getView().findViewById(R.id.tvProfileEmail));
            ImageView ivProfile = getView().findViewById(R.id.ivProfileImage);
            if (ivProfile != null && getContext() != null) {
                ivProfile.setImageResource(SharedPrefManager.getUserAvatar(getContext()));
            }
        }
    }

    private void updateProfileInfo(TextView tvName, TextView tvEmail) {
        if (!isAdded() || getContext() == null) return;
        String name = SharedPrefManager.getUserName(getContext());
        String email = SharedPrefManager.getUserEmail(getContext());
        
        if (SharedPrefManager.isGuest(getContext())) {
            name = "Guest";
            email = "Not logged in";
        } else if (name == null || name.isEmpty() || name.equalsIgnoreCase("Guest")) {
            if (email != null && !email.isEmpty() && email.contains("@")) {
                name = email.split("@")[0];
            } else {
                name = "User";
            }
        }
        
        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);
    }

    private void showMainOverflowMenu(View v) {
        if (getContext() == null) return;
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.menu_main_profile, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_help) {
                Toast.makeText(getContext(), "Support: support@dinear.com", Toast.LENGTH_LONG).show();
                return true;
            } else if (id == R.id.menu_privacy) {
                Toast.makeText(getContext(), "Privacy Policy coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_about) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("About DineAR")
                        .setMessage("DineAR v1.0\nAugmented Reality Food Platform\n\nBuilt for the future of dining.")
                        .setPositiveButton("Close", null)
                        .show();
                return true;
            } else if (id == R.id.menu_share) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Experience the future of dining with DineAR! Download now: https://dinear.com");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share DineAR via"));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void performLogout() {
        if (getContext() == null) return;
        
        // Clear all managers
        CartManager.getInstance().clear();
        FavoritesManager.getInstance().clear();
        
        SharedPrefManager.clearAuth(getContext());
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private void showChangeAvatarSheet(ImageView ivProfile) {
        if (!isAdded() || getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_change_avatar, null);
        dialog.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rvAvatarList);
        List<Integer> avatarList = new ArrayList<>();
        avatarList.add(R.drawable.avatar_0);
        avatarList.add(R.drawable.avatar_1);
        avatarList.add(R.drawable.avatar_2);
        avatarList.add(R.drawable.avatar_3);
        avatarList.add(R.drawable.avatar_4);
        avatarList.add(R.drawable.avatar_5);

        int currentAvatar = SharedPrefManager.getUserAvatar(getContext());
        tempSelectedAvatar = currentAvatar;

        AvatarAdapter adapter = new AvatarAdapter(getContext(), avatarList, currentAvatar, avatarResId -> tempSelectedAvatar = avatarResId);
        if (rv != null) rv.setAdapter(adapter);

        View close = view.findViewById(R.id.btnCloseAvatarSheet);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());
        View save = view.findViewById(R.id.btnSaveAvatar);
        if (save != null) save.setOnClickListener(v -> {
            if (getContext() != null) {
                SharedPrefManager.saveUserAvatar(getContext(), tempSelectedAvatar);
                ivProfile.setImageResource(tempSelectedAvatar);
                
                // Notify MainActivity to update its header image
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateProfileUI();
                }
                
                Toast.makeText(getContext(), "Avatar updated!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        dialog.show();
    }
}