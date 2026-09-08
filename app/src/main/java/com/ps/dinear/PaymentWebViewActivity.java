package com.ps.dinear;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ps.dinear.data.model.EsewaInitiateResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentWebViewActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private EsewaInitiateResponse esewaData;
    private String method;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_webview);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            esewaData = getIntent().getSerializableExtra("esewaData", EsewaInitiateResponse.class);
        } else {
            esewaData = (EsewaInitiateResponse) getIntent().getSerializableExtra("esewaData");
        }
        method = getIntent().getStringExtra("method");
        
        TextView tvTitle = findViewById(R.id.tvPaymentTitle);
        tvTitle.setText(method + " Payment");

        initViews();
        setupWebView();
        
        if (esewaData != null) {
            postToEsewa();
        } else {
            Toast.makeText(this, "Error: Payment data missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btnBackPayment).setOnClickListener(v -> finish());
    }

    private void initViews() {
        webView = findViewById(R.id.webViewPayment);
        progressBar = findViewById(R.id.pbPayment);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                if (url.contains(esewaData.getSuccessUrl())) {
                    verifyPayment();
                    return true;
                } else if (url.contains(esewaData.getFailureUrl())) {
                    Toast.makeText(PaymentWebViewActivity.this, "Payment failed or cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                    return true;
                }
                
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
    }

    private void postToEsewa() {
        try {
            StringBuilder sb = new StringBuilder();
            appendParam(sb, "amount", esewaData.getAmount());
            appendParam(sb, "tax_amount", esewaData.getTaxAmount());
            appendParam(sb, "total_amount", esewaData.getTotalAmount());
            appendParam(sb, "transaction_uuid", esewaData.getTransactionUuid());
            appendParam(sb, "product_code", esewaData.getProductCode());
            appendParam(sb, "product_service_charge", esewaData.getProductServiceCharge());
            appendParam(sb, "product_delivery_charge", esewaData.getProductDeliveryCharge());
            appendParam(sb, "success_url", esewaData.getSuccessUrl());
            appendParam(sb, "failure_url", esewaData.getFailureUrl());
            appendParam(sb, "signed_field_names", esewaData.getSignedFieldNames());
            appendParam(sb, "signature", esewaData.getSignature());

            webView.postUrl(esewaData.getEsewaUrl(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Toast.makeText(this, "Error preparing payment form", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void appendParam(StringBuilder sb, String key, String value) throws Exception {
        if (sb.length() > 0) sb.append("&");
        sb.append(URLEncoder.encode(key, "UTF-8"));
        sb.append("=");
        sb.append(URLEncoder.encode(value, "UTF-8"));
    }

    private void verifyPayment() {
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        Map<String, String> data = new HashMap<>();
        data.put("transaction_uuid", esewaData.getTransactionUuid());

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.verifyEsewaPayment(token, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    CartManager.getInstance().clear();
                    Toast.makeText(PaymentWebViewActivity.this, "Payment successful! Order placed.", Toast.LENGTH_LONG).show();
                    
                    Intent intent = new Intent(PaymentWebViewActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(PaymentWebViewActivity.this, "Payment verification failed", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PaymentWebViewActivity.this, "Network error during verification", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
