/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2018 The DotOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotos.updater;

import android.content.Intent;
import android.os.Bundle;

import com.dotos.updater.fragments.official.ChangelogFragment;
import com.dotos.updater.fragments.official.HomeFragment;
import com.dotos.updater.fragments.SettingsFragment;
import com.dotos.updater.misc.Utils;
import com.dotos.updater.ui.CustomViewPager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UpdatesActivity extends UpdatesListActivity {

    /*
     * 0 - Official
     * 1 - Nightly
     * 2 - Custom {url}
     */

    private TextView dashboardText;
    private CustomViewPager Opager, Npager;
    private int tempID = 0;
    private RelativeLayout confirmLayout;
    private TextView confirmtxt;
    String official;
    String nightly;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onResume() {
        super.onResume();
        if (tempID != Utils.getChannelID(this)) {
            confirmLayout.setVisibility(View.VISIBLE);
            confirmtxt.setText(getString(R.string.changed_channel_summary, Utils.getChannelID(this) == 0 ? official : nightly));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switch (Utils.getThemeID(this)) {
            case 0:
                setTheme(R.style.AppTheme);
                break;
            case 1:
                setTheme(R.style.AppThemeDark);
                break;
        }
        setContentView(R.layout.activity_base);
        official = getString(R.string.channel_offical);
        nightly = getString(R.string.channel_nightly);
        dashboardText = findViewById(R.id.header_title);
        confirmLayout = findViewById(R.id.confirmlayout);
        confirmtxt = findViewById(R.id.ch_changed);
        Button confirmbtn = findViewById(R.id.confirmbtn);
        setDashboardTitle(getString(R.string.title_updates));
        Chip ota_switch = findViewById(R.id.channel_selector);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tempID = Utils.getChannelID(this);
        confirmbtn.setOnClickListener(v -> {
            confirmLayout.setVisibility(View.GONE);
            tempID = Utils.getChannelID(this);
            if (tempID == 1) {
                Opager.setVisibility(View.GONE);
                Npager.setVisibility(View.VISIBLE);
                bottomNavigationView.setOnNavigationItemSelectedListener(this::onNNavigationItemSelected);
            } else {
                Opager.setVisibility(View.VISIBLE);
                Npager.setVisibility(View.GONE);
                bottomNavigationView.setOnNavigationItemSelectedListener(this::onONavigationItemSelected);
            }
            setDashboardTitle(getString(R.string.title_updates));
        });
        ota_switch.setOnClickListener(v -> startActivity(new Intent(this, ChannelSwitchActivity.class)));
        Opager = findViewById(R.id.ota_official);
        Npager = findViewById(R.id.ota_nightly);
        Opager.setAdapter(new OfficialAdapter(getSupportFragmentManager()));
        Npager.setAdapter(new NightlyAdapter(getSupportFragmentManager()));
        Npager.setScroll(false);
        Opager.setScroll(false);
        bottomNavigationView.inflateMenu(R.menu.menu_bottom);
        bottomNavigationView.setItemTextAppearanceActive(R.style.TextAppearance_Active);
        bottomNavigationView.setItemTextAppearanceInactive(R.style.TextAppearance_Inactive);
        bottomNavigationView.setItemIconTintList(getColorStateList(R.color.bottom_image_color));
        if (tempID == 0) {
            Opager.setVisibility(View.VISIBLE);
            Npager.setVisibility(View.GONE);
            bottomNavigationView.setOnNavigationItemSelectedListener(this::onONavigationItemSelected);
        } else {
            Opager.setVisibility(View.GONE);
            Npager.setVisibility(View.VISIBLE);
            bottomNavigationView.setOnNavigationItemSelectedListener(this::onNNavigationItemSelected);
        }
    }

    private boolean onONavigationItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.b_home:
                Opager.setCurrentItem(0, false);
                setDashboardTitle(getString(R.string.title_updates));
                return true;
            case R.id.b_changelog:
                Opager.setCurrentItem(1, false);
                setDashboardTitle(R.string.title_changelog);
                return true;
            case R.id.b_settings:
                Opager.setCurrentItem(2, false);
                setDashboardTitle(R.string.title_settings);
                return true;
        }
        return false;
    }

    private boolean onNNavigationItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.b_home:
                Npager.setCurrentItem(0, false);
                setDashboardTitle(getString(R.string.title_updates));
                return true;
            case R.id.b_changelog:
                Npager.setCurrentItem(1, false);
                setDashboardTitle(R.string.title_changelog);
                return true;
            case R.id.b_settings:
                Npager.setCurrentItem(2, false);
                setDashboardTitle(R.string.title_settings);
                return true;
        }
        return false;
    }

    public void setDashboardTitle(int resID) {
        dashboardText.setText(resID);
    }

    public void setDashboardTitle(String str) {
        dashboardText.setText(str);
    }

    @Override
    public void showSnackbar(int stringId, int duration) {
        Snackbar snack = Snackbar.make(findViewById(R.id.foocontainer),
                stringId, duration);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                snack.getView().getLayoutParams();
        params.setMargins(24,0, 24, bottomNavigationView.getHeight() + 24);
        snack.getView().setLayoutParams(params);
        snack.show();
    }

    public class OfficialAdapter extends FragmentPagerAdapter {

        OfficialAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new ChangelogFragment();
                case 2:
                    return new SettingsFragment();
                default:
                    return new HomeFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    public class NightlyAdapter extends FragmentPagerAdapter {

        NightlyAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new com.dotos.updater.fragments.nightly.HomeFragment();
                case 1:
                    return new com.dotos.updater.fragments.nightly.ChangelogFragment();
                case 2:
                    return new SettingsFragment();
                default:
                    return new com.dotos.updater.fragments.nightly.HomeFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

}
