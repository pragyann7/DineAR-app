package com.ps.dinear;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        findViewById(R.id.btnBackCategory).setOnClickListener(v -> finish());

        setupTag(R.id.catVeg, "Veg Restaurants");
        setupTag(R.id.catNonVeg, "Non Veg Restaurants");
        setupTag(R.id.catFastFood, "Fast Food");
        setupTag(R.id.catCafe, "Café");
        setupTag(R.id.catFineDining, "Fine Dining");
        setupTag(R.id.catBakery, "Bakery");
        setupTag(R.id.catNepali, "Nepali");
        setupTag(R.id.catIndian, "Indian");
        setupTag(R.id.catChinese, "Chinese");
        setupTag(R.id.catPizza, "Pizza");
    }

    private void setupTag(int id, String categoryName) {
        findViewById(id).setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("selected_category", categoryName);
            setResult(RESULT_OK, data);
            finish();
        });
    }
}
