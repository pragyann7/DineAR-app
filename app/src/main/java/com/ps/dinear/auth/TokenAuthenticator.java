package com.ps.dinear.auth;

import android.content.Context;
import com.ps.dinear.ApiService;
import com.ps.dinear.RetrofitClient;
import com.ps.dinear.SharedPrefManager;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class TokenAuthenticator implements Authenticator {
    private Context context;

    public TokenAuthenticator(Context context) {
        this.context = context;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // 1. Get refresh token
        String refreshToken = SharedPrefManager.getRefreshToken(context);
        if (refreshToken == null) return null;

        // 2. Call refresh endpoint synchronously
        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        Call<TokenResponse> call = api.refreshToken(new RefreshRequest(refreshToken));
        retrofit2.Response<TokenResponse> res = call.execute();

        if (res.isSuccessful() && res.body() != null) {
            // 3. Save new tokens
            TokenResponse tokens = res.body();
            SharedPrefManager.saveTokens(context, tokens.getAccess(), tokens.getRefresh());
            
            // 4. Retry original request with new token
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + tokens.getAccess())
                    .build();
        } else {
            // Refresh failed - log out user
            SharedPrefManager.clearAuth(context);
            return null;
        }
    }
}
