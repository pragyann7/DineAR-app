package com.ps.dinear;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class SearchFilterBottomSheet extends BottomSheetDialogFragment {

    private FilterListener listener;
    private String selectedSort = "";
    private float minRating = 0f;

    public interface FilterListener {
        void onFiltersApplied(String sort, float minRating);
    }

    public SearchFilterBottomSheet(FilterListener listener, String currentSort, float currentRating) {
        this.listener = listener;
        this.selectedSort = currentSort;
        this.minRating = currentRating;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_filter_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup cgSort = view.findViewById(R.id.cgSort);
        ChipGroup cgRating = view.findViewById(R.id.cgRating);

        // Pre-select current values
        if (selectedSort.equals("rating")) cgSort.check(R.id.chipRating);
        else if (selectedSort.equals("distance")) cgSort.check(R.id.chipDistance);
        else if (selectedSort.equals("price")) cgSort.check(R.id.chipPriceLow);

        if (minRating == 4.0f) cgRating.check(R.id.chipRating4);
        else if (minRating == 4.5f) cgRating.check(R.id.chipRating45);

        view.findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            cgSort.clearCheck();
            cgRating.clearCheck();
        });

        view.findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            int sortId = cgSort.getCheckedChipId();
            if (sortId == R.id.chipRating) selectedSort = "rating";
            else if (sortId == R.id.chipDistance) selectedSort = "distance";
            else if (sortId == R.id.chipPriceLow) selectedSort = "price";
            else selectedSort = "";

            int ratingId = cgRating.getCheckedChipId();
            if (ratingId == R.id.chipRating4) minRating = 4.0f;
            else if (ratingId == R.id.chipRating45) minRating = 4.5f;
            else minRating = 0f;

            listener.onFiltersApplied(selectedSort, minRating);
            dismiss();
        });
    }
}
