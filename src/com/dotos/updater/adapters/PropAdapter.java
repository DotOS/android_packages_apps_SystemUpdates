package com.dotos.updater.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dotos.updater.R;
import com.dotos.updater.model.Prop;

import java.util.List;


public class PropAdapter extends RecyclerView.Adapter<PropAdapter.ViewHolder> {

    private List<Prop> mList;

    public PropAdapter(List<Prop> mList) {
        this.mList = mList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_prop, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Prop app = mList.get(position);
        holder.desc.setText(app.getDesc());
        holder.value.setText(app.getValue());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView desc, value;

        ViewHolder(View view) {
            super(view);
            desc = view.findViewById(R.id.prop_desc);
            value = view.findViewById(R.id.prop_value);
        }
    }


}