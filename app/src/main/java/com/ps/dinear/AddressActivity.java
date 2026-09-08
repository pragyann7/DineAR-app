package com.ps.dinear;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ps.dinear.data.model.UserAddress;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressActivity extends AppCompatActivity {

    private RecyclerView rvAddresses;
    private AddressAdapter adapter;
    private List<UserAddress> addressList = new ArrayList<>();
    private LinearLayout llEmpty;
    private ProgressBar progressBar;
    
    private EditText tempEtTitle, tempEtLine, tempEtCity, tempEtDistrict;
    private static final int REQ_MAP_PICKER = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        rvAddresses = findViewById(R.id.rvAddresses);
        llEmpty = findViewById(R.id.llEmptyAddresses);
        progressBar = findViewById(R.id.pbAddress);

        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        
        findViewById(R.id.btnBackAddress).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddAddress).setOnClickListener(v -> showAddAddressDialog());

        loadAddresses();
    }

    private void loadAddresses() {
        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.getAddresses(token).enqueue(new Callback<List<UserAddress>>() {
            @Override
            public void onResponse(Call<List<UserAddress>> call, Response<List<UserAddress>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    addressList = response.body();
                    updateUI();
                } else {
                    Toast.makeText(AddressActivity.this, "Failed to load addresses", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserAddress>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String baseUrl = RetrofitClient.getBaseUrl(AddressActivity.this);
                Toast.makeText(AddressActivity.this, "Network error: " + t.getMessage() + "\nURL: " + baseUrl, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUI() {
        if (addressList.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvAddresses.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvAddresses.setVisibility(View.VISIBLE);
            
            adapter = new AddressAdapter(this, addressList, new AddressAdapter.AddressListener() {
                @Override
                public void onAddressSelected(UserAddress address) {
                    if (getIntent().getBooleanExtra("pickMode", false)) {
                        android.content.Intent intent = new android.content.Intent();
                        intent.putExtra("selectedAddress", address);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }

                @Override
                public void onAddressDelete(UserAddress address) {
                    deleteAddress(address.getId());
                }
            });
            rvAddresses.setAdapter(adapter);
        }
    }

    private void showAddAddressDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_address, null);
        tempEtTitle = view.findViewById(R.id.etAddressTitle);
        tempEtLine = view.findViewById(R.id.etAddressLine);
        tempEtCity = view.findViewById(R.id.etAddressCity);
        tempEtDistrict = view.findViewById(R.id.etAddressDistrict);
        
        view.findViewById(R.id.btnPickOnMap).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, MapAddressPickerActivity.class);
            startActivityForResult(intent, REQ_MAP_PICKER);
        });

        new AlertDialog.Builder(this)
                .setTitle("Add New Address")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = tempEtTitle.getText().toString().trim();
                    String line = tempEtLine.getText().toString().trim();
                    String city = tempEtCity.getText().toString().trim();
                    String district = tempEtDistrict.getText().toString().trim();

                    if (!title.isEmpty() && !line.isEmpty()) {
                        UserAddress newAddress = new UserAddress();
                        newAddress.setTitle(title);
                        newAddress.setAddressLine(line);
                        newAddress.setCity(city);
                        newAddress.setDistrict(district);
                        newAddress.setDefault(addressList.isEmpty());
                        
                        saveAddress(newAddress);
                    } else {
                        Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MAP_PICKER && resultCode == RESULT_OK && data != null) {
            String addressLine = data.getStringExtra("addressLine");
            String city = data.getStringExtra("city");
            String district = data.getStringExtra("district");
            
            if (tempEtLine != null) tempEtLine.setText(addressLine);
            if (tempEtCity != null) tempEtCity.setText(city);
            if (tempEtDistrict != null) tempEtDistrict.setText(district);
        }
    }

    private void saveAddress(UserAddress address) {
        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.addAddress(token, address).enqueue(new Callback<UserAddress>() {
            @Override
            public void onResponse(Call<UserAddress> call, Response<UserAddress> response) {
                if (response.isSuccessful()) {
                    loadAddresses();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddressActivity.this, "Failed to save address", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserAddress> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAddress(int id) {
        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + SharedPrefManager.getAccessToken(this);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        api.deleteAddress(token, id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadAddresses();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddressActivity.this, "Failed to delete address", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
