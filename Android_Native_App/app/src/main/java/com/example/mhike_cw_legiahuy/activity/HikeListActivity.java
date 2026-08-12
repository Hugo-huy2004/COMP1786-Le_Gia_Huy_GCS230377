package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.adapter.HikeAdapter;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.ui.BottomNav;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HikeListActivity extends BaseActivity {

    private RecyclerView recycler;
    private TextView txtCount;
    private TabLayout tabs;
    private HikeAdapter adapter;
    private final List<Hike> allHikes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike_list);

        recycler = findViewById(R.id.recycler);
        txtCount = findViewById(R.id.txtCount);
        tabs = findViewById(R.id.tabs);

        adapter = new HikeAdapter(new HikeAdapter.Listener() {
            @Override public void onOpen(Hike h) {
                Intent i = new Intent(HikeListActivity.this, HikeDetailActivity.class);
                i.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, h.id);
                startActivity(i);
            }
            @Override public void onLongPress(Hike h, View anchor) { showItemMenu(h, anchor); }
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.fab).setOnClickListener(v ->
                startActivity(new Intent(this, CreateHikeActivity.class)));
        
        findViewById(R.id.btnDeleteAll).setOnClickListener(v -> confirmDeleteAll());
        
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { reload(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        BottomNav.wire(this, 1);
        listen();
    }

    private void listen() {
        FirebaseHelper.hikes().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Hike> all = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) { h.id = s.getKey(); all.add(h); }
                }
                allHikes.clear();
                allHikes.addAll(all);
                reload();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("MHike", "hike list cancelled: " + error.getMessage());
            }
        });
    }

    /** Applies the selected tab and refreshes the counts. Both read the stored status. */
    private void reload() {
        int completed = 0, planned = 0;
        for (Hike h : allHikes) {
            if (isCompleted(h)) completed++; else planned++;
        }
        txtCount.setText(getString(R.string.hikes_count_fmt, completed, planned));

        int tab = tabs == null ? 0 : tabs.getSelectedTabPosition();
        List<Hike> shown = new ArrayList<>();
        for (Hike h : allHikes) {
            boolean keep;
            switch (tab) {
                case 1: keep = !isCompleted(h); break;   // Planned
                case 2: keep = isCompleted(h); break;    // Completed
                case 3: keep = h.favourite; break;       // Favourites
                default: keep = true;                    // All
            }
            if (keep) shown.add(h);
        }
        adapter.submitList(shown);
    }

    private static boolean isCompleted(Hike h) {
        return h.status != null && "completed".equalsIgnoreCase(h.status.trim());
    }

    private void showItemMenu(Hike h, View anchor) {
        String edit = getString(R.string.action_edit);
        String fav = getString(h.favourite ? R.string.action_unfavourite : R.string.action_favourite);
        String status = getString(isCompleted(h) ? R.string.action_mark_planned : R.string.action_mark_completed);

        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(edit);
        menu.getMenu().add(fav);
        menu.getMenu().add(status);
        menu.getMenu().add(R.string.action_delete);
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if (title.equals(edit)) {
                Intent i = new Intent(this, CreateHikeActivity.class);
                i.putExtra(CreateHikeActivity.EXTRA_HIKE_ID, h.id);
                startActivity(i);
            } else if (title.equals(fav)) {
                // Stored as 0/1 to stay readable by the web app, same as Hike.getFavouriteRaw().
                FirebaseHelper.hikes().child(h.id).child("favourite").setValue(h.favourite ? 0 : 1);
            } else if (title.equals(status)) {
                FirebaseHelper.hikes().child(h.id).child("status")
                        .setValue(isCompleted(h) ? "planned" : "completed");
            } else {
                deleteWithUndo(h);
            }
            return true;
        });
        menu.show();
    }

    private void deleteWithUndo(final Hike h) {
        FirebaseHelper.hikes().child(h.id).removeValue();
        Snackbar.make(findViewById(R.id.root), getString(R.string.deleted_fmt, h.name), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> FirebaseHelper.hikes().child(h.id).setValue(h))
                .show();
    }

    private void confirmDeleteAll() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_all)
                .setPositiveButton(R.string.delete_all_hikes, (d, w) -> FirebaseHelper.hikes().removeValue())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
