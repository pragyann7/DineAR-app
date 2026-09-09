package com.ps.dinear;

import com.ps.dinear.auth.ForgotPasswordRequest;
import com.ps.dinear.auth.LoginRequest;
import com.ps.dinear.auth.RefreshRequest;
import com.ps.dinear.auth.RegistrationRequest;
import com.ps.dinear.auth.RegistrationResponse;
import com.ps.dinear.auth.ResendOtpRequest;
import com.ps.dinear.auth.ResetPasswordRequest;
import com.ps.dinear.auth.TokenResponse;
import com.ps.dinear.auth.VerifyOtpRequest;
import com.ps.dinear.data.model.FavoriteIdsResponse;
import com.ps.dinear.data.model.FavoriteRequest;
import com.ps.dinear.data.model.Order;
import com.ps.dinear.data.model.OrderRequest;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.data.model.Review;
import com.ps.dinear.data.model.ReviewListResponse;
import com.ps.dinear.data.model.RestaurantMenuResponse;
import com.ps.dinear.data.model.SearchResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/restaurants/")
    Call<List<Restaurant>> getRestaurants(
        @Query("city") String city,
        @Query("category") String category
    );

    @GET("api/restaurants/featured/")
    Call<List<Restaurant>> getFeaturedRestaurants();

    @GET("api/restaurants/categories/")
    Call<List<Restaurant.Category>> getCategories();

    @GET("api/restaurants/{slug}/")
    Call<Restaurant> getRestaurantDetails(@Path("slug") String slug);

    @GET("api/menu/restaurant/{slug}/")
    Call<RestaurantMenuResponse> getMenu(@Path("slug") String slug);

    @GET("api/search/")
    Call<SearchResponse> search(
        @Query("q") String query,
        @Query("city") String city
    );

    @GET("api/favorites/ids/")
    Call<FavoriteIdsResponse> getFavoriteIds(@Header("Authorization") String token);

    @GET("api/favorites/details/")
    Call<SearchResponse> getFavoriteDetails(@Header("Authorization") String token);

    @POST("api/favorites/")
    Call<Void> addFavorite(@Header("Authorization") String token, @Body FavoriteRequest request);

    @DELETE("api/favorites/{type}/{id}/")
    Call<Void> removeFavorite(
        @Header("Authorization") String token,
        @Path("type") String type,
        @Path("id") int id
    );

    @POST("api/auth/register/")
    Call<RegistrationResponse> register(@Body RegistrationRequest request);

    @POST("api/auth/verify-email-otp/")
    Call<Void> verifyEmailOtp(@Body VerifyOtpRequest request);

    @POST("api/auth/resend-email-otp/")
    Call<Void> resendEmailOtp(@Body ResendOtpRequest request);

    @POST("api/auth/login/")
    Call<TokenResponse> login(@Body LoginRequest request);

    @POST("api/auth/token/refresh/")
    Call<TokenResponse> refreshToken(@Body RefreshRequest request);

    @POST("api/auth/forgot-password/")
    Call<Void> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/reset-password/")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    @GET("api/auth/me/")
    Call<com.ps.dinear.auth.ProfileResponse> getProfile(@Header("Authorization") String token);

    @PATCH("api/auth/me/")
    Call<com.ps.dinear.auth.ProfileResponse> updateProfile(@Header("Authorization") String token, @Body java.util.Map<String, Object> fields);

    @DELETE("api/auth/me/")
    Call<Void> deleteAccount(@Header("Authorization") String token);

    @POST("api/auth/change-password/")
    Call<Void> changePassword(@Header("Authorization") String token, @Body java.util.Map<String, String> data);

    @GET("api/auth/addresses/")
    Call<List<com.ps.dinear.data.model.UserAddress>> getAddresses(@Header("Authorization") String token);

    @POST("api/auth/addresses/")
    Call<com.ps.dinear.data.model.UserAddress> addAddress(
        @Header("Authorization") String token, 
        @Body com.ps.dinear.data.model.UserAddress address
    );

    @DELETE("api/auth/addresses/{id}/")
    Call<Void> deleteAddress(@Header("Authorization") String token, @Path("id") int id);

    @POST("api/orders/")
    Call<Order> placeOrder(@Header("Authorization") String token, @Body OrderRequest request);

    @POST("api/payments/create/")
    Call<com.ps.dinear.data.model.EsewaInitiateResponse> initiateEsewaPayment(
        @Header("Authorization") String token, 
        @Body com.ps.dinear.data.model.PaymentRequest request
    );

    @POST("api/payments/esewa/verify/")
    Call<Void> verifyEsewaPayment(
        @Header("Authorization") String token,
        @Body java.util.Map<String, String> data
    );

    @GET("api/orders/")
    Call<List<Order>> getOrders(@Header("Authorization") String token);

    @GET("api/orders/{id}/")
    Call<Order> getOrderDetails(@Header("Authorization") String token, @Path("id") int id);

    @GET("api/reviews/")
    Call<ReviewListResponse> getReviews(
        @Header("Authorization") String token,
        @Query("restaurant_id") Integer restaurantId,
        @Query("food_item_id") Integer foodItemId,
        @Query("include_summary") boolean includeSummary,
        @Query("page") Integer page,
        @Query("ordering") String ordering,
        @Query("filter") String filter,
        @Query("city") String city
    );

    @POST("api/reviews/")
    Call<Review> postReview(
        @Header("Authorization") String token,
        @Body Review review
    );

    @POST("api/reviews/{id}/helpful/")
    Call<java.util.Map<String, Object>> toggleHelpful(
        @Header("Authorization") String token,
        @Path("id") int reviewId
    );
}
