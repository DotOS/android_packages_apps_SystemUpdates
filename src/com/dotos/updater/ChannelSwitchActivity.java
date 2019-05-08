package com.dotos.updater;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dotos.updater.adapters.ChannelAdapter;
import com.dotos.updater.misc.Utils;
import com.dotos.updater.model.ChannelVerified;

import java.util.ArrayList;

public class ChannelSwitchActivity extends AppCompatActivity {

    ArrayList<ChannelVerified> vf = new ArrayList<>();
    RecyclerView verifiedRecycler, customRecycler;

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
        setContentView(R.layout.fragment_channel);
        verifiedRecycler = findViewById(R.id.verifiedRecycler);
        customRecycler = findViewById(R.id.customRecycler);
        vf.add(new ChannelVerified("OFFICIAL", "Only for official devices", getString(R.string.updater_server_url)));
        vf.add(new ChannelVerified("Nightly", "Nightly builds by maintainers", getString(R.string.updater_server_url)));
        ChannelAdapter adapter = new ChannelAdapter(vf, true);
        verifiedRecycler.setAdapter(adapter);
        verifiedRecycler.setLayoutManager(new LinearLayoutManager(this));
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

}
