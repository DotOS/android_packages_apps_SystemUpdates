/*
 * Copyright (C) 2017-2020 The LineageOS Project
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
package com.dot.systemupdates;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dot.systemupdates.controller.UpdaterController;
import com.dot.systemupdates.controller.UpdaterService;
import com.dot.systemupdates.download.DownloadClient;
import com.dot.systemupdates.misc.Constants;
import com.dot.systemupdates.misc.Utils;
import com.dot.systemupdates.model.UpdateInfo;
import com.dot.systemupdates.ui.PreferenceSheet;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdatesActivity extends UpdatesListActivity {

    private static final String TAG = "UpdatesActivity";
    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private Animation bounceAnimation;
    private CollapsingToolbarLayout headerTitle;

    private UpdateView updateView;
    private RelativeLayout actionCheck;
    private SwipeRefreshLayout pullToRefresh;
    private int impatience = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updates);
        actionCheck = findViewById(R.id.actionCheck);
        pullToRefresh = findViewById(R.id.updates_swipe_container);
        RelativeLayout actionStart = findViewById(R.id.actionStart);
        LinearLayout actionOptions = findViewById(R.id.actionOptions);
        RelativeLayout updateProgress = findViewById(R.id.updateProgressLayout);
        RelativeLayout actionInstall = findViewById(R.id.actionInstall);
        RelativeLayout actionReboot = findViewById(R.id.actionReboot);
        RelativeLayout actionDelete = findViewById(R.id.actionDelete);

        updateView = findViewById(R.id.updateView);
        updateView.setupControlViews(actionCheck, actionStart, updateProgress, actionOptions, actionInstall, actionReboot, actionDelete);
        updateView.setActivity(this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    handleDownloadStatusChange(downloadId);
                    updateView.lateInit();
                } else if (UpdaterController.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction()) ||
                        UpdaterController.ACTION_INSTALL_PROGRESS.equals(intent.getAction()) ||
                        UpdaterController.ACTION_UPDATE_REMOVED.equals(intent.getAction())) {
                    updateView.lateInit();
                }
            }
        };
        headerTitle = findViewById(R.id.app_bar);

        TextView settingsLauncher = findViewById(R.id.settingsButton);
        settingsLauncher.setClickable(true);
        settingsLauncher.setOnClickListener(v -> showPreferencesDialog());

        bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce);

        actionCheck.findViewById(R.id.actionCheckButton).setOnClickListener(view -> downloadUpdatesList(true));
        pullToRefresh.setOnRefreshListener(() -> downloadUpdatesList(true));
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, UpdaterService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        if (mUpdaterService != null) {
            unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            updateView.setUpdaterController(mUpdaterService.getUpdaterController());
            getUpdatesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            updateView.setUpdaterController(null);
            mUpdaterService = null;
            updateView.lateInit();
        }
    };

    private void loadUpdatesList(File jsonFile, boolean manualRefresh)
            throws IOException, JSONException {
        Log.d(TAG, "Adding remote updates");
        UpdaterController controller = mUpdaterService.getUpdaterController();
        boolean newUpdates = false;

        List<UpdateInfo> updates = Utils.parseJson(jsonFile, true);
        List<String> updatesOnline = new ArrayList<>();
        for (UpdateInfo update : updates) {
            newUpdates |= controller.addUpdate(update);
            updatesOnline.add(update.getDownloadId());
        }
        controller.setUpdatesAvailableOnline(updatesOnline, true);

        if (manualRefresh) {
            impatience++;
            updateView.unleashTheBunny((!newUpdates && impatience >= 3 ? R.string.bunny : R.string.nothing));
            if (newUpdates) {
                updateView.unleashTheBunny(R.string.hit);
            }
        }

        List<String> updateIds = new ArrayList<>();
        List<UpdateInfo> sortedUpdates = controller.getUpdates();
        if (sortedUpdates.isEmpty()) {
            updateView.setDownloadId(null);
            updateView.noUpdates();
            actionCheck.setVisibility(View.VISIBLE);
        } else {
            sortedUpdates.sort((u1, u2) -> Long.compare(u2.getTimestamp(), u1.getTimestamp()));
            for (UpdateInfo update : sortedUpdates) {
                updateIds.add(update.getDownloadId());
            }
            headerTitle.setTitle(getString(R.string.snack_updates_found));
            actionCheck.setVisibility(View.GONE);
            updateView.setDownloadId(updateIds.get(0));
        }
    }

    private void getUpdatesList() {
        File jsonFile = Utils.getCachedUpdateList(this);
        if (jsonFile.exists()) {
            try {
                loadUpdatesList(jsonFile, false);
                Log.d(TAG, "Cached list parsed");
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error while parsing json list", e);
            }
        } else {
            downloadUpdatesList(false);
        }
    }

    private void processNewJson(File json, File jsonNew, boolean manualRefresh) {
        try {
            loadUpdatesList(jsonNew, manualRefresh);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            long millis = System.currentTimeMillis();
            preferences.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, millis).apply();
            if (json.exists() && Utils.isUpdateCheckEnabled(this) &&
                    Utils.checkForNewUpdates(json, jsonNew)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(this);
            }
            // In case we set a one-shot check because of a previous failure
            UpdatesCheckReceiver.cancelUpdatesCheck(this);
            jsonNew.renameTo(json);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read json", e);
            showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
        }
    }

    private void downloadUpdatesList(final boolean manualRefresh) {
        final File jsonFile = Utils.getCachedUpdateList(this);
        final File jsonFileTmp = new File(jsonFile.getAbsolutePath() + UUID.randomUUID());
        String url = Utils.getServerURL(this);
        Log.d(TAG, "Checking " + url);

        DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
            @Override
            public void onFailure(final boolean cancelled) {
                Log.e(TAG, "Could not download updates list");
                runOnUiThread(() -> {
                    if (!cancelled) {
                        showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
                    }
                    refreshAnimationStop();
                    pullToRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(DownloadClient.Headers headers) {
            }

            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Log.d(TAG, "List downloaded");
                    processNewJson(jsonFile, jsonFileTmp, manualRefresh);
                    refreshAnimationStop();
                    pullToRefresh.setRefreshing(false);
                });
            }
        };

        final DownloadClient downloadClient;
        try {
            downloadClient = new DownloadClient.Builder()
                    .setUrl(url)
                    .setDestination(jsonFileTmp)
                    .setDownloadCallback(callback)
                    .build();
        } catch (IOException exception) {
            Log.e(TAG, "Could not build download client");
            showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
            return;
        }

        refreshAnimationStart();
        downloadClient.start();
    }

    private void handleDownloadStatusChange(String downloadId) {
        UpdateInfo update = mUpdaterService.getUpdaterController().getUpdate(downloadId);
        switch (update.getStatus()) {
            case PAUSED_ERROR:
                showSnackbar(R.string.snack_download_failed, Snackbar.LENGTH_LONG);
                break;
            case VERIFICATION_FAILED:
                showSnackbar(R.string.snack_download_verification_failed, Snackbar.LENGTH_LONG);
                break;
            case VERIFIED:
                showSnackbar(R.string.snack_download_verified, Snackbar.LENGTH_LONG);
                break;
        }
    }

    @Override
    public void showSnackbar(int stringId, int duration) {
        Snackbar.make(findViewById(R.id.main_container), stringId, duration).show();
    }

    private void refreshAnimationStart() {
        bounceAnimation.setRepeatCount(Animation.INFINITE);
        actionCheck.startAnimation(bounceAnimation);
        actionCheck.setEnabled(false);
    }

    private void refreshAnimationStop() {
        bounceAnimation.setRepeatCount(0);
        actionCheck.setEnabled(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showPreferencesDialog() {
        new PreferenceSheet().setupPreferenceSheet(mUpdaterService).show(getSupportFragmentManager(), "prefdialog");
    }
}
