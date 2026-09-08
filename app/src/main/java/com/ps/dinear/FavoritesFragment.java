package com.ps.dinear;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class FavoritesFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TabLayoutMediator tabLayoutMediator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayoutFav);
        viewPager = view.findViewById(R.id.viewPagerFav);

        // Professional Standard: Dynamically adjust to Activity Header height
        view.post(() -> {
            if (getActivity() instanceof MainActivity) {
                int totalHeaderHeight = ((MainActivity) getActivity()).getHeaderTotalHeight();
                if (tabLayout != null && totalHeaderHeight > 0) {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = 
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) tabLayout.getLayoutParams();
                    lp.topMargin = totalHeaderHeight;
                    tabLayout.setLayoutParams(lp);
                }
            }
        });

        if (viewPager != null && tabLayout != null) {
            FavoritesPagerAdapter pagerAdapter = new FavoritesPagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);
            viewPager.setOffscreenPageLimit(2);

            tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                if (position == 0) tab.setText("Restaurants");
                else tab.setText("Foods");
            });
            tabLayoutMediator.attach();
        }
    }

    @Override
    public void onDestroyView() {
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
            tabLayoutMediator = null;
        }
        if (viewPager != null) {
            viewPager.setAdapter(null);
            viewPager = null;
        }
        super.onDestroyView();
    }

    private static class FavoritesPagerAdapter extends FragmentStateAdapter {
        public FavoritesPagerAdapter(@NonNull Fragment fragment) {
            super(fragment.getChildFragmentManager(), fragment.getLifecycle());
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return FavoriteListFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}