package com.example.mhike_cw_legiahuy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.util.DiffColor;

public class HikeAdapter extends ListAdapter<Hike, HikeAdapter.VH> {

    public interface Listener {
        void onOpen(Hike h);
        void onLongPress(Hike h, View anchor);
    }

    private final Listener listener;

    public HikeAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public long getItemId(int position) {
        String id = getItem(position).id;
        return id == null ? position : id.hashCode();
    }

    private static final DiffUtil.ItemCallback<Hike> DIFF = new DiffUtil.ItemCallback<Hike>() {
        @Override public boolean areItemsTheSame(@NonNull Hike a, @NonNull Hike b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull Hike a, @NonNull Hike b) {
            return eq(a.name, b.name) && a.lengthKm == b.lengthKm && eq(a.hikeDate, b.hikeDate)
                    && eq(a.difficulty, b.difficulty);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hike, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Hike hike = getItem(position);
        h.name.setText(hike.name);
        h.meta.setText(meta(hike));
        
        h.tagDifficulty.setText(hike.difficulty == null ? "" : hike.difficulty);
        h.tagDifficulty.setVisibility(hike.difficulty == null ? View.GONE : View.VISIBLE);
        
        android.content.res.Resources res = h.tagDifficulty.getResources();
        h.tagDifficulty.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(res.getColor(DiffColor.solid(hike.difficulty), null)));

        h.tagDistance.setText(h.itemView.getContext().getString(R.string.stat_km_fmt, trim(hike.lengthKm)));

        if (h.star != null) {
            h.star.setVisibility(hike.favourite ? View.VISIBLE : View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onOpen(hike));
        h.itemView.setOnLongClickListener(v -> { listener.onLongPress(hike, v); return true; });
    }

    private static String meta(Hike h) {
        StringBuilder sb = new StringBuilder();
        if (h.hikeDate != null) {
            sb.append(h.hikeDate);
        }
        return sb.toString();
    }

    private static String trim(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    private static boolean eq(Object a, Object b) { return a == null ? b == null : a.equals(b); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, meta, tagDifficulty, tagDistance;
        final android.widget.ImageView star;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.txtName);
            meta = v.findViewById(R.id.txtMeta);
            tagDifficulty = v.findViewById(R.id.tagDifficulty);
            tagDistance = v.findViewById(R.id.tagDistance);
            star = v.findViewById(R.id.imgStar);
        }
    }
}
