package com.ps.dinear;

import android.content.Context;
import android.util.Log;

import com.ps.dinear.data.model.FavoriteIdsResponse;
import com.ps.dinear.data.model.FavoriteRequest;

import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesManager {
    private static final String TAG = "FavoritesManager";
    private static FavoritesManager instance;
    private Set<Integer> favoriteRestaurantIds = new HashSet<>();
    private Set<Integer> favoriteFoodIds = new HashSet<>();
    private boolean isLoaded = false;

    private FavoritesManager() {}

    public static synchronized FavoritesManager getInstance() {
        if (instance == null) {
            instance = new FavoritesManager();
        }
        return instance;
    }

    public void loadFavorites(Context context) {
        String token = SharedPrefManager.getAccessToken(context);
        if (token == null) return;

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        api.getFavoriteIds("Bearer " + token).enqueue(new Callback<FavoriteIdsResponse>() {
            @Override
            public void onResponse(Call<FavoriteIdsResponse> call, Response<FavoriteIdsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    favoriteRestaurantIds.clear();
                    favoriteRestaurantIds.addAll(response.body().getRestaurantIds());
                    favoriteFoodIds.clear();
                    favoriteFoodIds.addAll(response.body().getFoodIds());
                    isLoaded = true;
                    Log.d(TAG, "Favorites loaded successfully");
                }
            }

            @Override
            public void onFailure(Call<FavoriteIdsResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load favorites", t);
            }
        });
    }

    public boolean isRestaurantFavorite(int id) {
        return favoriteRestaurantIds.contains(id);
    }

    public boolean isFoodFavorite(int id) {
        return favoriteFoodIds.contains(id);
    }

    public void toggleRestaurantFavorite(Context context, int id, ToggleCallback callback) {
        boolean isCurrentlyFav = isRestaurantFavorite(id);
        
        // Optimistic UI update
        if (isCurrentlyFav) favoriteRestaurantIds.remove(id);
        else favoriteRestaurantIds.add(id);
        callback.onStateChanged(!isCurrentlyFav);

        String token = SharedPrefManager.getAccessToken(context);
        if (token == null) {
            callback.onError("Please login to favorite restaurants");
            // Revert optimistic update
            if (isCurrentlyFav) favoriteRestaurantIds.add(id);
            else favoriteRestaurantIds.remove(id);
            callback.onStateChanged(isCurrentlyFav);
            return;
        }

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        if (isCurrentlyFav) {
            api.removeFavorite("Bearer " + token, "restaurant", id).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        favoriteRestaurantIds.add(id);
                        callback.onStateChanged(true);
                        callback.onError("Failed to remove favorite");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteRestaurantIds.add(id);
                    callback.onStateChanged(true);
                    callback.onError("Network error");
                }
            });
        } else {
            api.addFavorite("Bearer " + token, new FavoriteRequest("restaurant", id)).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        favoriteRestaurantIds.remove(id);
                        callback.onStateChanged(false);
                        callback.onError("Failed to add favorite");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteRestaurantIds.remove(id);
                    callback.onStateChanged(false);
                    callback.onError("Network error");
                }
            });
        }
    }

    public void toggleFoodFavorite(Context context, int id, ToggleCallback callback) {
        boolean isCurrentlyFav = isFoodFavorite(id);
        
        // Optimistic UI update
        if (isCurrentlyFav) favoriteFoodIds.remove(id);
        else favoriteFoodIds.add(id);
        callback.onStateChanged(!isCurrentlyFav);

        String token = SharedPrefManager.getAccessToken(context);
        if (token == null) {
            callback.onError("Please login to favorite foods");
            // Revert optimistic update
            if (isCurrentlyFav) favoriteFoodIds.add(id);
            else favoriteFoodIds.remove(id);
            callback.onStateChanged(isCurrentlyFav);
            return;
        }

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        if (isCurrentlyFav) {
            api.removeFavorite("Bearer " + token, "food", id).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        favoriteFoodIds.add(id);
                        callback.onStateChanged(true);
                        callback.onError("Failed to remove favorite");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteFoodIds.add(id);
                    callback.onStateChanged(true);
                    callback.onError("Network error");
                }
            });
        } else {
            api.addFavorite("Bearer " + token, new FavoriteRequest("food", id)).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        favoriteFoodIds.remove(id);
                        callback.onStateChanged(false);
                        callback.onError("Failed to add favorite");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteFoodIds.remove(id);
                    callback.onStateChanged(false);
                    callback.onError("Network error");
                }
            });
        }
    }

    public interface ToggleCallback {
        void onStateChanged(boolean isFavorite);
        void onError(String message);
    }
}
