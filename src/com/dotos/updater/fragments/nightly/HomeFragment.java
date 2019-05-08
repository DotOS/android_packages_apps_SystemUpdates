package com.dotos.updater.fragments.nightly;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.dotos.updater.R;
import com.dotos.updater.UpdatesCheckReceiver;
import com.dotos.updater.UpdatesListActivity;
import com.dotos.updater.UpdatesListAdapter;
import com.dotos.updater.adapters.PropAdapter;
import com.dotos.updater.controller.UpdaterController;
import com.dotos.updater.controller.UpdaterService;
import com.dotos.updater.download.DownloadClient;
import com.dotos.updater.misc.BuildInfoUtils;
import com.dotos.updater.misc.Constants;
import com.dotos.updater.misc.Utils;
import com.dotos.updater.model.Prop;
import com.dotos.updater.model.UpdateBase;
import com.dotos.updater.model.UpdateInfo;
import com.dotos.updater.ui.RoundedDialog;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HomeFragment extends Fragment {

    private List<Prop> propList = new ArrayList<>();
    private List<Prop> maintainerInfo = new ArrayList<>();
    private RecyclerView infoRecycler, recyclerView, maintainerRecycler;
    private LinearLayout no_update_layout, info_layout;
    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;
    private UpdatesListAdapter mAdapter;
    private ImageButton mRefreshIconView;
    private String TAG = "HomeFragment";
    private TextView otaTitle;
    private ImageButton maintainerDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ota_nightly_home, container, false);
        infoRecycler = view.findViewById(R.id.changelogRecycler);
        recyclerView = view.findViewById(R.id.updateRecycler);
        mRefreshIconView = view.findViewById(R.id.ota_check);
        no_update_layout = view.findViewById(R.id.no_update_layout);
        info_layout = view.findViewById(R.id.uinfo);
        maintainerRecycler = view.findViewById(R.id.maintainerRecycler);
        otaTitle = view.findViewById(R.id.ota_check_title);
        maintainerDialog = view.findViewById(R.id.maintainer_info);
        propList.clear();
        maintainerInfo.clear();
        propList.add(new Prop(getString(R.string.build_version), BuildInfoUtils.getBuildVersion()));
        propList.add(new Prop(getString(R.string.build_time), timeFormat(Integer.parseInt(String.valueOf(BuildInfoUtils.getBuildDateTimestamp())))));
        propList.add(new Prop(getString(R.string.build_type), SystemProperties.get(Constants.PROP_RELEASE_TYPE)));
        propList.add(new Prop(getString(R.string.device), SystemProperties.get("ro.product.device")));
        propList.removeIf(prop -> prop.getValue().equals(""));
        PropAdapter propAdapter = new PropAdapter(propList);
        infoRecycler.setAdapter(propAdapter);
        infoRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        initUpdater();
        ((TextView) view.findViewById(R.id.ota_current)).setText(String.format("%s • %s", timeFormat(Integer.parseInt(String.valueOf(BuildInfoUtils.getBuildDateTimestamp()))), BuildInfoUtils.getBuildVersion()));
        mRefreshIconView.setOnClickListener(v -> downloadUpdatesList(true));
        return view;
    }

    private void initUpdater() {
        mAdapter = new UpdatesListAdapter((UpdatesListActivity) getActivity(), mRefreshIconView);
        recyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    handleDownloadStatusChange(downloadId);
                    mAdapter.notifyDataSetChanged();
                } else if (UpdaterController.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction()) ||
                        UpdaterController.ACTION_INSTALL_PROGRESS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    mAdapter.notifyItemChanged(downloadId);
                } else if (UpdaterController.ACTION_UPDATE_REMOVED.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    mAdapter.removeItem(downloadId);
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), UpdaterService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        if (mUpdaterService != null) {
            getActivity().unbindService(mConnection);
        }
        super.onStop();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            mAdapter.setUpdaterController(mUpdaterService.getUpdaterController());
            getUpdatesList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mAdapter.setUpdaterController(null);
            mUpdaterService = null;
            mAdapter.notifyDataSetChanged();
        }
    };

    private void loadUpdatesList(File jsonFile, boolean manualRefresh)
            throws IOException, JSONException {
        UpdaterController controller = mUpdaterService.getUpdaterController();
        boolean newUpdates = false;

        ArrayList<UpdateBase> changes = new ArrayList<>();

        List<UpdateInfo> updates = Utils.parseNightlyJson(jsonFile, true);
        List<String> updatesOnline = new ArrayList<>();
        for (UpdateInfo update : updates) {
            newUpdates |= controller.addUpdate(update);
            updatesOnline.add(update.getDownloadId());
        }

        controller.setUpdatesAvailableOnline(updatesOnline, true);

        if (manualRefresh) {
            showSnackbar(
                    newUpdates ? R.string.snack_updates_found : R.string.snack_no_updates_found,
                    Snackbar.LENGTH_SHORT);
        }

        List<String> updateIds = new ArrayList<>();
        List<UpdateInfo> sortedUpdates = controller.getUpdates();
        ArrayList<UpdateBase> tempCheck = Utils.getNightlyChangelog(getActivity());
        if (sortedUpdates.isEmpty()) {
            //otaTitle.setText(getString(R.string.list_no_updates));
            recyclerView.setVisibility(View.GONE);
            info_layout.setVisibility(View.GONE);
            no_update_layout.setVisibility(View.VISIBLE);
        } else {
            maintainerDialog.setOnClickListener(v -> {
                View rootView = View.inflate(getContext(), R.layout.dialog_maintainer, null);
                RoundedDialog dialog = new RoundedDialog(getActivity(), R.style.Theme_RoundedDialog);
                Button openTG, closeD;
                closeD = rootView.findViewById(R.id.closeDialog);
                closeD.setOnClickListener(v2 -> dialog.dismiss());
                openTG = rootView.findViewById(R.id.opentTG);
                openTG.setOnClickListener(v3 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sortedUpdates.get(0).getMaintainerTelegram()))));
                TextView name, devices;
                name = rootView.findViewById(R.id.mtName);
                devices = rootView.findViewById(R.id.mtDevice);
                name.setText(sortedUpdates.get(0).getMaintainerName());
                devices.setText(getString(R.string.maintainer_devices, sortedUpdates.get(0).getMaintainerDevice()));
                ImageView img;
                img = rootView.findViewById(R.id.mtProfile);
                Glide.with(getActivity())
                        .load(sortedUpdates.get(0).getMaintainerImageUrl())
                        .into(img);
                dialog.setContentView(rootView);
                dialog.show();
            });
            otaTitle.setText(getString(R.string.new_updates_found_title));
            info_layout.setVisibility(View.VISIBLE);
            no_update_layout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            sortedUpdates.sort((u1, u2) -> Long.compare(u2.getTimestamp(), u1.getTimestamp()));
            for (UpdateInfo update : sortedUpdates) {
                updateIds.add(update.getDownloadId());
            }
            mAdapter.setData(updateIds);
            mAdapter.notifyDataSetChanged();
            maintainerInfo.clear();
            maintainerInfo.add(new Prop("Name", sortedUpdates.get(0).getMaintainerName()));
            maintainerInfo.add(new Prop("XDA", sortedUpdates.get(0).getMaintainerXDA()));
            maintainerInfo.add(new Prop("Other", sortedUpdates.get(0).getMaintainerOther()));
            maintainerRecycler.setAdapter(new PropAdapter(maintainerInfo));
            maintainerRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            if (tempCheck != null && tempCheck.size() > 0) {
                UpdateBase temp = tempCheck.get(0);
                if (temp.getTimestamp() < sortedUpdates.get(0).getTimestamp()) {
                    UpdateBase changelog = new UpdateBase();
                    changelog.setTimestamp(sortedUpdates.get(0).getTimestamp());
                    changelog.setC_System(sortedUpdates.get(0).getSystemChangelog());
                    changelog.setC_Settings(sortedUpdates.get(0).getSettingsChangelog());
                    changelog.setC_Device(sortedUpdates.get(0).getDeviceChangelog());
                    changelog.setC_SecPatch(sortedUpdates.get(0).getSecurityPatchChangelog());
                    changelog.setC_Misc(sortedUpdates.get(0).getMiscChangelog());
                    changes.add(changelog);
                    Utils.pushNightlyChangelog(getActivity(), changes);
                }
            } else {
                UpdateBase changelog = new UpdateBase();
                changelog.setTimestamp(sortedUpdates.get(0).getTimestamp());
                changelog.setC_System(sortedUpdates.get(0).getSystemChangelog());
                changelog.setC_Settings(sortedUpdates.get(0).getSettingsChangelog());
                changelog.setC_Device(sortedUpdates.get(0).getDeviceChangelog());
                changelog.setC_SecPatch(sortedUpdates.get(0).getSecurityPatchChangelog());
                changelog.setC_Misc(sortedUpdates.get(0).getMiscChangelog());
                changes.add(changelog);
                Utils.pushNightlyChangelog(getActivity(), changes);
            }
        }
    }

    private void getUpdatesList() {
        File jsonFile = Utils.getCachedNightlyUpdateList(getContext());
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
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            long millis = System.currentTimeMillis();
            preferences.edit().putLong(Constants.PREF_LAST_UPDATE_CHECK, millis).apply();
            updateLastCheckedString();
            if (json.exists() && preferences.getBoolean(Constants.PREF_AUTO_UPDATES_CHECK, true) &&
                    Utils.checkForNewUpdates(json, jsonNew)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(getContext());
            }
            // In case we set a one-shot check because of a previous failure
            UpdatesCheckReceiver.cancelUpdatesCheck(getContext());
            jsonNew.renameTo(json);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read json", e);
            showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
        }
    }

    private void downloadUpdatesList(final boolean manualRefresh) {
        final File jsonFile = Utils.getCachedNightlyUpdateList(getContext());
        final File jsonFileTmp = new File(jsonFile.getAbsolutePath() + UUID.randomUUID());
        String url = Utils.getNightlyServerURL(getContext());
        Log.d(TAG, "Checking " + url);

        DownloadClient.DownloadCallback callback = new DownloadClient.DownloadCallback() {
            @Override
            public void onFailure(final boolean cancelled) {
                Log.e(TAG, "Could not download updates list");
                getActivity().runOnUiThread(() -> {
                    if (!cancelled) {
                        showSnackbar(R.string.snack_updates_check_failed, Snackbar.LENGTH_LONG);
                    }
                });
            }

            @Override
            public void onResponse(int statusCode, String url,
                                   DownloadClient.Headers headers) {
            }

            @Override
            public void onSuccess(File destination) {
                getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "List downloaded");
                    processNewJson(jsonFile, jsonFileTmp, manualRefresh);
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

        downloadClient.start();
    }

    private void updateLastCheckedString() {
            /*final SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            long lastCheck = preferences.getLong(Constants.PREF_LAST_UPDATE_CHECK, -1) / 1000;
            String lastCheckString = getString(R.string.header_last_updates_check,
                    StringGenerator.getDateLocalized(getContext(), DateFormat.LONG, lastCheck),
                    StringGenerator.getTimeLocalized(getContext(), lastCheck));
            mChangelogSheet.initDetailsText(new TextView(getContext()));
            mChangelogSheet.setDetails(
                    getString(R.string.header_android_version, Build.VERSION.RELEASE),
                    StringGenerator.getDateLocalizedUTC(getContext(), DateFormat.LONG, BuildInfoUtils.getBuildDateTimestamp()),
                    lastCheckString);*/
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

    private void showSnackbar(int stringId, int duration) {
        Snackbar.make(getView().findViewById(R.id.snackProvider), stringId, duration).show();
    }

    private String timeFormat(int timestamp) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp * 1000L);
        return DateFormat.format("E, MMM d", cal).toString();
    }
}