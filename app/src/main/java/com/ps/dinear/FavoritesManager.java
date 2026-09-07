package com.ps.dinear;

import android.content.Context;
import android.util.Log;

import com.ps.dinear.data.model.FavoriteIdsResponse;
import com.ps.dinear.data.model.FavoriteRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    
    private final List<FavoritesListener> listeners = new ArrayList<>();

    public interface FavoritesListener {
        void onFavoritesUpdated();
    }

    private FavoritesManager() {}

    public static synchronized FavoritesManager getInstance() {
        if (instance == null) {
            instance = new FavoritesManager();
        }
        return instance;
    }
    
    public void addListener(FavoritesListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }
    
    public void removeListener(FavoritesListener l) {
        listeners.remove(l);
    }
    
    private void notifyListeners() {
        for (FavoritesListener l : listeners) {
            l.onFavoritesUpdated();
        }
    }

    public void loadFavorites(Context context) {
        String token = SharedPrefManager.getAccessToken(context);
        if (token == null) {
            favoriteRestaurantIds.clear();
            favoriteFoodIds.clear();
            notifyListeners();
            return;
        }

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
                    Log.d(TAG, "Favorites loaded: Restaurants=" + favoriteRestaurantIds.size() + ", Foods=" + favoriteFoodIds.size());
                    notifyListeners();
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
        
        if (isCurrentlyFav) favoriteRestaurantIds.remove(id);
        else favoriteRestaurantIds.add(id);
        callback.onStateChanged(!isCurrentlyFav);
        notifyListeners();

        String token = SharedPrefManager.getAccessToken(context);
        if (token == null) {
            callback.onError("Please login to favorite restaurants");
            if (isCurrentlyFav) favoriteRestaurantIds.add(id);
            else favoriteRestaurantIds.remove(id);
            callback.onStateChanged(isCurrentlyFav);
            notifyListeners();
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
                        notifyListeners();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteRestaurantIds.add(id);
                    callback.onStateChanged(true);
                    callback.onError("Network error");
                    notifyListeners();
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
                        notifyListeners();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteRestaurantIds.remove(id);
                    callback.onStateChanged(false);
                    callback.onError("Network error");
                    notifyListeners();
                }
            });
        }
    }

    public void toggleFoodFavorite(Context context, int id, ToggleCallback callback) {
        boolean isCurrentlyFav = isFoodFavorite(id);
        
        if (isCurrentlyFav) favoriteFoodIds.remove(id);
        else favoriteFoodIds.add(id);
        callback.onStateChanged(!isCurrentlyFav);
        notifyListeners();

        String token = SharedPrefManager.getAccessToken(context);
        if (token == null) {
            callback.onError("Please login to favorite foods");
            if (isCurrentlyFav) favoriteFoodIds.add(id);
            else favoriteFoodIds.remove(id);
            callback.onStateChanged(isCurrentlyFav);
            notifyListeners();
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
                        notifyListeners();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteFoodIds.add(id);
                    callback.onStateChanged(true);
                    callback.onError("Network error");
                    notifyListeners();
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
                        notifyListeners();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    favoriteFoodIds.remove(id);
                    callback.onStateChanged(false);
                    callback.onError("Network error");
                    notifyListeners();
                }
            });
        }
    }

    public interface ToggleCallback {
        void onStateChanged(boolean isFavorite);
        void onError(String message);
    }
}
