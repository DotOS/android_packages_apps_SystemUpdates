package com.dotos.updater.fragments.nightly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dotos.updater.R;
import com.dotos.updater.adapters.PropAdapter;
import com.dotos.updater.misc.Utils;
import com.dotos.updater.model.Prop;
import com.dotos.updater.model.UpdateBase;

import java.util.ArrayList;
import java.util.List;

public class ChangelogFragment extends Fragment {

        List<Prop> items = new ArrayList<>();
        ArrayList<UpdateBase> changelog;
        RecyclerView recyclerView;

        @Override
        public void onResume() {
            items.clear();
            changelog = Utils.getChangelog(getActivity());
            if (changelog != null) {
                UpdateBase storedInfo = changelog.get(0);
                items.add(new Prop("System", storedInfo.getSystemChangelog() != null ? storedInfo.getSystemChangelog() : ""));
                items.add(new Prop("Settings", storedInfo.getSettingsChangelog() != null ? storedInfo.getSettingsChangelog() : ""));
                items.add(new Prop("Device", storedInfo.getDeviceChangelog() != null ? storedInfo.getDeviceChangelog() : ""));
                items.add(new Prop("Security Patch", storedInfo.getSecurityPatchChangelog() != null ? storedInfo.getSecurityPatchChangelog() : ""));
                items.add(new Prop("Misc", storedInfo.getMiscChangelog() != null ? storedInfo.getMiscChangelog() : ""));
                items.removeIf(prop -> prop.getValue().equals(""));
                PropAdapter adapter = new PropAdapter(items);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }
            super.onResume();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.ota_nightly_changelog, container, false);
            recyclerView = view.findViewById(R.id.changelogRecycler);
            return view;
        }
    }