package com.dotos.updater.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dotos.updater.R;
import com.dotos.updater.misc.Utils;
import com.dotos.updater.model.ChannelBase;
import com.dotos.updater.model.ChannelVerified;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    private List<ChannelVerified> mList;
    private boolean verified;

    public ChannelAdapter(List<ChannelVerified> mList, boolean verified) {
        this.mList = mList;
        this.verified = verified;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (verified)
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        else
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel_custom, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        ChannelBase app = mList.get(position);
        if (verified) {
            holder.title.setText(app.getName());
            holder.summary.setText(app.getSummary());
            app.setSelected(Utils.getChannelID(holder.layout.getContext()) == position);
            holder.selectedView.setVisibility(app.isSelected() ? View.VISIBLE : View.INVISIBLE);
            holder.layout.setOnClickListener(v -> {
                Utils.setChannelID(holder.layout.getContext(), position);
                app.setSelected(true);
                holder.selectedView.setVisibility(View.VISIBLE);
                for (int i = 0; i<mList.size(); i++) {
                    if (i != position) {
                        ChannelBase appr = mList.get(i);
                        appr.setSelected(false);
                        holder.selectedView.setVisibility(View.INVISIBLE);
                        notifyItemChanged(i);
                    }
                }
                notifyItemChanged(position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, summary;
        ImageView selectedView;
        ImageButton removeCustom;
        RelativeLayout layout;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.ch_name);
            summary = view.findViewById(R.id.ch_summary);
            selectedView = view.findViewById(R.id.ch_selected);
            layout = view.findViewById(R.id.confirmlayout);
            if (verified)
                removeCustom = view.findViewById(R.id.ch_remove);
        }
    }


}