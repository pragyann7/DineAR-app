package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class ServerConfigActivity
        extends AppCompatActivity {

    EditText etIp;
    EditText etPort;

    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(
                R.layout.activity_server_config
        );

        etIp = findViewById(R.id.etIp);
        etPort = findViewById(R.id.etPort);

        btnSave = findViewById(R.id.btnSave);

        String savedIp =
                SharedPrefManager.getIp(this);

        String savedPort =
                SharedPrefManager.getPort(this);

        if (savedIp != null) {
            etIp.setText(savedIp);
        }

        etPort.setText(savedPort);

        btnSave.setOnClickListener(v -> {

            String ip =
                    etIp.getText()
                            .toString()
                            .trim();

            String port =
                    etPort.getText()
                            .toString()
                            .trim();

            SharedPrefManager.saveServer(
                    this,
                    ip,
                    port
            );

            String baseUrl =
                    "http://" +
                            ip +
                            ":" +
                            port +
                            "/";

            RetrofitClient.initialize(
                    baseUrl,
                    this
            );

            startActivity(
                    new Intent(
                            this,
                            MainActivity.class
                    )
            );

            finish();
        });
    }
}