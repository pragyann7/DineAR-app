package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.ps.dinear.data.model.SearchResponse;
import com.ps.dinear.location.LocationActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private View navHome, navNearby, navFavorites, navProfile;
    private ImageView ivHome, ivNearby, ivFavorites, ivProfile;
    private TextView tvHome, tvNearby, tvFavorites, tvProfile;
    
    private MenuItem featuredDish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color and light status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (SharedPrefManager.getDistrict(this) == null) {
            startActivity(new Intent(this, LocationActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Global Data Initialization
        FavoritesManager.getInstance().loadFavorites(this);

        // Handle navigation bar insets for bottom navigation and AR button
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

        setupNavigation();
        setupFloatingARButton();
        fetchFeaturedDish();
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
            startActivity(new Intent(this, DiscoverActivity.class));
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
                if (destination.getId() == R.id.homeFragment) updateNavUI(0);
                else if (destination.getId() == R.id.favoritesFragment) updateNavUI(2);
                else if (destination.getId() == R.id.profileFragment) updateNavUI(3);
            });
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
            if (featuredDish != null) {
                Intent intent = new Intent(this, ARActivity.class);
                intent.putExtra("selectedItem", featuredDish);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Finding a featured dish for you...", Toast.LENGTH_SHORT).show();
            }
        });
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
                        
                        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                        if (navHost != null) {
                            for (Fragment f : navHost.getChildFragmentManager().getFragments()) {
                                if (f instanceof HomeFragment && f.isAdded()) {
                                    ((HomeFragment) f).setFeaturedDish(featuredDish, null, -1);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {}
        });
    }
}
