package com.ps.dinear;

import com.ps.dinear.auth.LoginRequest;
import com.ps.dinear.auth.RegistrationRequest;
import com.ps.dinear.auth.TokenResponse;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.SearchResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/restaurants/")
    Call<List<Restaurant>> getRestaurants(
        @Query("city") String city,
        @Query("category") String category
    );

    @GET("api/restaurants/{id}/")
    Call<Restaurant> getRestaurantDetails(@Path("id") int id);

    @GET("api/menu-items/")
    Call<List<MenuItem>> getMenu(@Query("restaurant") Integer restaurantId);

    @GET("api/search/")
    Call<SearchResponse> search(
        @Query("q") String query,
        @Query("city") String city
    );

    @POST("api/register/")
    Call<Void> register(@Body RegistrationRequest request);

    @POST("api/token/")
    Call<TokenResponse> login(@Body LoginRequest request);
}