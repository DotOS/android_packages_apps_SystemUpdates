package com.dotos.updater.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.dotos.updater.R;
import com.dotos.updater.UpdatesCheckReceiver;
import com.dotos.updater.controller.UpdaterService;
import com.dotos.updater.misc.Constants;
import com.dotos.updater.misc.Utils;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private Switch autoCheck;
    private Switch autoDelete;
    private Switch dataWarning;
    private Switch abPerfMode;
    private Switch enableDark;
    UpdaterService mUpdaterService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        enableDark = view.findViewById(R.id.preferences_enable_dark);
        autoCheck = view.findViewById(R.id.preferences_auto_updates_check);
        autoDelete = view.findViewById(R.id.preferences_auto_delete_updates);
        dataWarning = view.findViewById(R.id.preferences_mobile_data_warning);
        abPerfMode = view.findViewById(R.id.preferences_ab_perf_mode);

        if (!Utils.isABDevice()) {
            abPerfMode.setVisibility(View.GONE);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        enableDark.setChecked(Utils.getThemeID(getContext()) == 1);
        autoCheck.setChecked(prefs.getBoolean(Constants.PREF_AUTO_UPDATES_CHECK, true));
        autoDelete.setChecked(prefs.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false));
        dataWarning.setChecked(prefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true));
        abPerfMode.setChecked(prefs.getBoolean(Constants.PREF_AB_PERF_MODE, false));
        autoCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit()
                    .putBoolean(Constants.PREF_AUTO_UPDATES_CHECK,
                            autoCheck.isChecked())
                    .apply();
            if (autoCheck.isChecked()) {
                UpdatesCheckReceiver.scheduleRepeatingUpdatesCheck(getContext());
            } else {
                UpdatesCheckReceiver.cancelRepeatingUpdatesCheck(getContext());
                UpdatesCheckReceiver.cancelUpdatesCheck(getContext());
            }
        });
        autoDelete.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit()
                .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES,
                        autoDelete.isChecked())
                .apply());
        dataWarning.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit()
                .putBoolean(Constants.PREF_MOBILE_DATA_WARNING,
                        dataWarning.isChecked())
                .apply());
        abPerfMode.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit()
                .putBoolean(Constants.PREF_AB_PERF_MODE,
                        abPerfMode.isChecked())
                .apply());
        enableDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.setThemeID(getContext(), isChecked ? 1 : 0);
            Intent intent = getActivity().getIntent();
            getActivity().overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            getActivity().finish();
            getActivity().overridePendingTransition(0, 0);
            getActivity().startActivity(intent);
        });
        return view;
    }

}
