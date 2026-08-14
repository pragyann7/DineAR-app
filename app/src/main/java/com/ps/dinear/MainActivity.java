package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private MenuAdapter adapter;

    ProgressBar progressBar;
    private final List<MenuItem> menuItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String ip = SharedPrefManager.getIp(this);
        String port = SharedPrefManager.getPort(this);

        if (ip == null) {
            ip = "192.168.1.69";
        }

        if (port == null) {
            port = "8000";
        }

        String baseUrl =
                "http://" + ip + ":" + port + "/";

        RetrofitClient.initialize(baseUrl);
        setContentView(R.layout.activity_main);

        Button btnServer =
                findViewById(R.id.btnServer);

        btnServer.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            ServerConfigActivity.class
                    )
            );
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MenuAdapter(this);

        progressBar = findViewById(R.id.progressBar);

        recyclerView.setVisibility(View.GONE);

        recyclerView.setAdapter(adapter);

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        progressBar.setVisibility(View.VISIBLE);

        apiService.getMenu().enqueue(new Callback<List<MenuItem>>() {
            @Override
            public void onResponse(Call<List<MenuItem>> call,
                                   Response<List<MenuItem>> response) {

                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {

                    List<MenuItem> menuList = response.body();

                    adapter.setData(menuList);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<MenuItem>> call, Throwable t) {

                progressBar.setVisibility(View.GONE);

                Toast.makeText(MainActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}