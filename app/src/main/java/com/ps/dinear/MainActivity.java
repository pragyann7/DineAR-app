package com.ps.dinear;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.ps.dinear.data.model.SearchResponse;
import com.ps.dinear.location.LocationActivity;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private View navHome, navNearby, navFavorites, navProfile;
    private ImageView ivHome, ivNearby, ivFavorites, ivProfile;
    private TextView tvHome, tvNearby, tvFavorites, tvProfile;
    
    private View rlHeader, cvHomeProfile, llUserContext;
    private TextView tvWelcome, tvCurrentLocation, tvCartBadge, tvPageTitle;
    private ImageView ivHomeProfile;
    
    private View llGlobalNetworkError;
    private View btnGlobalRetry;
    private ProgressBar pbGlobalRetry;

    private MenuItem featuredDish;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final CartManager.CartListener cartListener = this::updateCartBadge;

    private final ActivityResultLauncher<Intent> discoverLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {}
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (SharedPrefManager.getDistrict(this) == null) {
            startActivity(new Intent(this, LocationActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        FavoritesManager.getInstance().loadFavorites(this);
        CartManager.getInstance().addListener(cartListener);

        initViews();
        setupInsets();
        setupNavigation();
        setupFloatingARButton();
        setupNetworkMonitoring();
        
        fetchFeaturedDish();
        updateWelcomeText();
        updateCartBadge();
    }

    private void initViews() {
        rlHeader = findViewById(R.id.rlHeader);
        cvHomeProfile = findViewById(R.id.cvHomeProfile);
        llUserContext = findViewById(R.id.llUserContext);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        ivHomeProfile = findViewById(R.id.ivHomeProfile);
        llGlobalNetworkError = findViewById(R.id.llGlobalNetworkError);
        btnGlobalRetry = findViewById(R.id.btnGlobalRetry);
        pbGlobalRetry = findViewById(R.id.pbGlobalRetry);

        findViewById(R.id.btnCart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        findViewById(R.id.btnChangeLocation).setOnClickListener(v -> startActivity(new Intent(this, LocationActivity.class)));
        cvHomeProfile.setOnClickListener(v -> {
            if (navController != null) navController.navigate(R.id.profileFragment);
        });
        
        btnGlobalRetry.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                setOverlayLoading(true);
                refreshActiveContent();
                fetchFeaturedDish();
            } else {
                Toast.makeText(this, "Still no connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setOverlayLoading(boolean loading) {
        if (btnGlobalRetry != null) btnGlobalRetry.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (pbGlobalRetry != null) pbGlobalRetry.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    public void showErrorOverlay() {
        showErrorOverlay(true);
    }

    public void showErrorOverlay(boolean withDelay) {
        handler.removeCallbacksAndMessages(null);

        Runnable showTask = () -> {
            if (navController != null && navController.getCurrentDestination() != null 
                    && navController.getCurrentDestination().getId() == R.id.profileFragment) {
                return;
            }

            setOverlayLoading(false);
            if (llGlobalNetworkError != null && llGlobalNetworkError.getVisibility() != View.VISIBLE) {
                llGlobalNetworkError.setAlpha(0f);
                llGlobalNetworkError.setVisibility(View.VISIBLE);
                llGlobalNetworkError.animate().alpha(1f).setDuration(300).start();
            }
        };

        if (withDelay) {
            handler.postDelayed(showTask, 1500);
        } else {
            showTask.run();
        }
    }

    public void hideErrorOverlay() {
        hideErrorOverlay(true);
    }

    public void hideErrorOverlay(boolean animate) {
        handler.removeCallbacksAndMessages(null);
        setOverlayLoading(false);
        if (llGlobalNetworkError == null || llGlobalNetworkError.getVisibility() == View.GONE) return;
        
        if (animate) {
            llGlobalNetworkError.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        llGlobalNetworkError.setVisibility(View.GONE);
                        llGlobalNetworkError.setAlpha(1f);
                    });
        } else {
            llGlobalNetworkError.setVisibility(View.GONE);
        }
    }

    private void refreshActiveContent() {
        Fragment f = getActiveFragment();
        if (f instanceof HomeFragment) {
            ((HomeFragment) f).refreshData();
        } else if (f instanceof FavoritesFragment) {
            List<Fragment> children = f.getChildFragmentManager().getFragments();
            for (Fragment child : children) {
                if (child instanceof FavoriteListFragment && child.isAdded() && child.isVisible()) {
                    ((FavoriteListFragment) child).refreshData();
                }
            }
        }
    }

    private void setupInsets() {
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
            androidx.core.graphics.Insets statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.getLayoutParams().height = statusBars.top;
            v.requestLayout();
            
            View bottomNav = findViewById(R.id.bottomNav);
            View arScan = findViewById(R.id.btnARScan);
            androidx.core.graphics.Insets navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            
            if (bottomNav != null) {
                float density = getResources().getDisplayMetrics().density;
                bottomNav.getLayoutParams().height = (int) (70 * density) + navBars.bottom;
                bottomNav.setPadding(bottomNav.getPaddingLeft(), (int) (4 * density), 
                                  bottomNav.getPaddingRight(), (int) (8 * density) + navBars.bottom);
                bottomNav.requestLayout();
            }
            
            if (arScan != null) {
                android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) arScan.getLayoutParams();
                float density = getResources().getDisplayMetrics().density;
                mlp.bottomMargin = (int) (35 * density) + navBars.bottom;
                arScan.setLayoutParams(mlp);
            }
            
            return insets;
        });
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        navHome = findViewById(R.id.navHome);
        navNearby = findViewById(R.id.navNearby);
        navFavorites = findViewById(R.id.navFavorites);
        navProfile = findViewById(R.id.navProfile);

        ivHome = findViewById(R.id.ivNavHome);
        ivNearby = findViewById(R.id.ivNavNearby);
        ivFavorites = findViewById(R.id.ivNavFavorites);
        ivProfile = findViewById(R.id.ivNavProfile);

        tvHome = findViewById(R.id.tvNavHome);
        tvNearby = findViewById(R.id.tvNavNearby);
        tvFavorites = findViewById(R.id.tvNavFavorites);
        tvProfile = findViewById(R.id.tvNavProfile);

        navHome.setOnClickListener(v -> {
            if (navController != null && (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.homeFragment)) {
                navController.navigate(R.id.homeFragment);
            }
        });

        navNearby.setOnClickListener(v -> {
            discoverLauncher.launch(new Intent(this, DiscoverActivity.class));
        });

        navFavorites.setOnClickListener(v -> {
            if (navController != null && (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.favoritesFragment)) {
                navController.navigate(R.id.favoritesFragment);
            }
        });

        navProfile.setOnClickListener(v -> {
            if (navController != null && (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.profileFragment)) {
                navController.navigate(R.id.profileFragment);
            }
        });

        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                int orangeColor = androidx.core.content.ContextCompat.getColor(this, R.color.orange_primary);
                
                if (destId == R.id.homeFragment) {
                    updateNavUI(0);
                    rlHeader.setVisibility(View.VISIBLE);
                    setHeaderState(null, true, true);
                    updateProfileUI();
                    checkAndRestoreErrorState();
                    
                    getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                            .setAppearanceLightStatusBars(true);
                } else if (destId == R.id.favoritesFragment) {
                    updateNavUI(2);
                    rlHeader.setVisibility(View.VISIBLE);
                    setHeaderState("My Favorites", false, false);
                    checkAndRestoreErrorState();
                    
                    getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                            .setAppearanceLightStatusBars(true);
                } else if (destId == R.id.profileFragment) {
                    updateNavUI(3);
                    rlHeader.setVisibility(View.GONE);
                    hideErrorOverlay(false);
                    
                    getWindow().setStatusBarColor(orangeColor);
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                            .setAppearanceLightStatusBars(false);
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController != null && navController.getCurrentDestination() != null) {
                    int currentId = navController.getCurrentDestination().getId();
                    if (currentId != R.id.homeFragment) {
                        navController.navigate(R.id.homeFragment);
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
            }
        });
    }

    private void checkAndRestoreErrorState() {
        if (!isNetworkAvailable()) {
            showErrorOverlay(false);
        }
    }

    private void setHeaderState(String title, boolean showProfile, boolean showContext) {
        if (rlHeader.getVisibility() == View.GONE) return;
        
        if (title != null) {
            tvPageTitle.setText(title);
            tvPageTitle.setVisibility(View.VISIBLE);
        } else {
            tvPageTitle.setVisibility(View.GONE);
        }

        cvHomeProfile.setVisibility(showProfile ? View.VISIBLE : View.GONE);
        llUserContext.setVisibility(showContext ? View.VISIBLE : View.GONE);
        
        float density = getResources().getDisplayMetrics().density;
        if (showProfile || showContext) {
            rlHeader.setPadding(rlHeader.getPaddingLeft(), (int)(16 * density), rlHeader.getPaddingRight(), (int)(12 * density));
        } else {
            rlHeader.setPadding(rlHeader.getPaddingLeft(), (int)(24 * density), rlHeader.getPaddingRight(), (int)(16 * density));
        }
    }

    private void updateNavUI(int selectedIndex) {
        int orange = androidx.core.content.ContextCompat.getColor(this, R.color.orange_primary);
        int gray = androidx.core.content.ContextCompat.getColor(this, R.color.gray_text);

        if (ivHome != null) ivHome.setColorFilter(selectedIndex == 0 ? orange : gray);
        if (tvHome != null) tvHome.setTextColor(selectedIndex == 0 ? orange : gray);

        if (ivNearby != null) ivNearby.setColorFilter(selectedIndex == 1 ? orange : gray);
        if (tvNearby != null) tvNearby.setTextColor(selectedIndex == 1 ? orange : gray);

        if (ivFavorites != null) ivFavorites.setColorFilter(selectedIndex == 2 ? orange : gray);
        if (tvFavorites != null) tvFavorites.setTextColor(selectedIndex == 2 ? orange : gray);

        if (ivProfile != null) ivProfile.setColorFilter(selectedIndex == 3 ? orange : gray);
        if (tvProfile != null) tvProfile.setTextColor(selectedIndex == 3 ? orange : gray);
    }

    private void setupFloatingARButton() {
        findViewById(R.id.btnARScan).setOnClickListener(v -> {
            if (!isNetworkAvailable() || (llGlobalNetworkError != null && llGlobalNetworkError.getVisibility() == View.VISIBLE)) {
                showErrorOverlay();
                Toast.makeText(this, "Something went wrong...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (featuredDish != null) {
                Intent intent = new Intent(this, ARActivity.class);
                intent.putExtra("selectedItem", featuredDish);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Finding a featured dish for you...", Toast.LENGTH_SHORT).show();
                fetchFeaturedDish();
            }
        });
    }

    private void setupNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    refreshActiveContent();
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> showErrorOverlay());
            }
        };
        
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
        
        if (!isNetworkAvailable()) {
            showErrorOverlay();
        }
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return true;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileUI();
        updateCartBadge();
        
        if (tvCurrentLocation != null) {
            String locationText = SharedPrefManager.getCity(this) + ", " + SharedPrefManager.getDistrict(this);
            tvCurrentLocation.setText(locationText);
        }
    }

    public void updateProfileUI() {
        updateWelcomeText();
        if (ivHomeProfile != null) {
            ivHomeProfile.setImageResource(SharedPrefManager.getUserAvatar(this));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CartManager.getInstance().removeListener(cartListener);
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void updateWelcomeText() {
        String name = SharedPrefManager.getUserName(this);
        String email = SharedPrefManager.getUserEmail(this);
        if (SharedPrefManager.isGuest(this)) {
            name = "Guest";
        } else if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Guest")) {
            if (email != null && email.contains("@")) {
                name = email.split("@")[0];
            } else {
                name = "User";
            }
        }
        
        // Only show the first name in the greeting
        if (name != null && name.contains(" ")) {
            name = name.split(" ")[0];
        }
        
        if (tvWelcome != null) {
            String displayName = (name != null) ? name.toUpperCase(Locale.getDefault()) : "USER";
            tvWelcome.setText(getString(R.string.hello_placeholder, displayName));
        }
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        int count = CartManager.getInstance().getItemCount();
        if (count > 0) {
            tvCartBadge.setText(String.valueOf(count));
            tvCartBadge.setVisibility(View.VISIBLE);
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    private Fragment getActiveFragment() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            List<Fragment> fragments = navHost.getChildFragmentManager().getFragments();
            if (!fragments.isEmpty()) {
                for (int i = fragments.size() - 1; i >= 0; i--) {
                    Fragment f = fragments.get(i);
                    if (f.isVisible()) return f;
                }
            }
        }
        return null;
    }

    private void fetchFeaturedDish() {
        String city = SharedPrefManager.getCity(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.search("", city).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MenuItem> foods = response.body().getFoodItems();
                    if (foods != null && !foods.isEmpty()) {
                        featuredDish = foods.get(0);
                        Fragment topFragment = getActiveFragment();
                        if (topFragment instanceof HomeFragment && topFragment.isAdded()) {
                            ((HomeFragment) topFragment).setFeaturedDish(featuredDish, null, -1);
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {}
        });
    }
}
