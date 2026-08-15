package com.ps.dinear.location;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import com.ps.dinear.MainActivity;
import com.ps.dinear.R;
import com.ps.dinear.SharedPrefManager;
import com.ps.dinear.data.model.Location;
import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity {

    private RecyclerView rvLocations;
    private LocationAdapter adapter;
    private List<Location> locationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        rvLocations = findViewById(R.id.rvLocations);
        SearchView searchView = findViewById(R.id.searchView);

        locationList = new ArrayList<>();
        locationList.add(new Location("Chitwan", "Bharatpur"));
        locationList.add(new Location("Kathmandu", "Kathmandu"));
        locationList.add(new Location("Kaski", "Pokhara"));
        locationList.add(new Location("Rupandehi", "Butwal"));
        locationList.add(new Location("Lalitpur", "Lalitpur"));
        locationList.add(new Location("Bhaktapur", "Bhaktapur"));
        locationList.add(new Location("Parsa", "Birgunj"));
        locationList.add(new Location("Morang", "Biratnagar"));

        adapter = new LocationAdapter(this, locationList, location -> {
            SharedPrefManager.saveLocation(this, location.getDistrict(), location.getCity());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        rvLocations.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }
}