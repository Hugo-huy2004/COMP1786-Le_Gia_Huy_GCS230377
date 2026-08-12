package com.example.mhike_cw_legiahuy.activity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.example.mhike_cw_legiahuy.util.HikeFilter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends BaseActivity {

    private EditText edtSearch;
    private RecyclerView recycler;
    private TextView txtResultHeader;
    private HikeAdapter adapter;
    private HikeFilter adv;
    private final List<Hike> allHikes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        edtSearch = findViewById(R.id.edtSearch);
        recycler = findViewById(R.id.recycler);
        txtResultHeader = findViewById(R.id.txtResultHeader);
        TabLayout searchTabs = findViewById(R.id.searchTabs);

        adapter = new HikeAdapter(new HikeAdapter.Listener() {
            @Override public void onOpen(Hike h) {
                startActivity(new Intent(SearchActivity.this, HikeDetailActivity.class)
                        .putExtra(HikeDetailActivity.EXTRA_HIKE_ID, h.id));
            }
            @Override public void onLongPress(Hike h, View anchor) {}
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        edtSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        if (searchTabs != null) {
            searchTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 1) showAdvanced();
                    else { adv = null; filter(edtSearch.getText().toString()); }
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 1) showAdvanced();
                }
            });
        }

        BottomNav.wire(this, 2);

        edtSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT);
        
        loadAllData();
    }

    private void loadAllData() {
        FirebaseHelper.hikes().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allHikes.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) { h.id = s.getKey(); allHikes.add(h); }
                }
                filter(edtSearch.getText().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.UK);
        List<Hike> results = new ArrayList<>();
        for (Hike h : allHikes) {
            boolean textMatch = HikeFilter.contains(h.name, q) || HikeFilter.contains(h.location, q);
            if (textMatch && (adv == null || adv.accepts(h))) results.add(h);
        }
        adapter.submitList(results);
        if (results.isEmpty()) {
            txtResultHeader.setText(R.string.no_match);
        } else {
            txtResultHeader.setText(adv == null
                    ? getString(R.string.results_filtered_fmt, results.size())
                    : getString(R.string.filters_applied_fmt, results.size()));
        }
    }

    /** The sheet layout already existed; this just fills it in and reads it back. */
    private void showAdvanced() {
        View sheet = getLayoutInflater().inflate(R.layout.sheet_advanced_search, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheet);

        EditText name = sheet.findViewById(R.id.edtAdvName);
        EditText location = sheet.findViewById(R.id.edtAdvLocation);
        EditText minKm = sheet.findViewById(R.id.edtMinKm);
        EditText maxKm = sheet.findViewById(R.id.edtMaxKm);
        EditText from = sheet.findViewById(R.id.edtDateFrom);
        EditText to = sheet.findViewById(R.id.edtDateTo);

        if (adv != null) {
            name.setText(adv.name);
            location.setText(adv.location);
            if (adv.minKm != null) minKm.setText(String.valueOf(adv.minKm));
            if (adv.maxKm != null) maxKm.setText(String.valueOf(adv.maxKm));
            from.setText(adv.from);
            to.setText(adv.to);
        }

        from.setOnClickListener(v -> pickDate(from));
        to.setOnClickListener(v -> pickDate(to));

        sheet.findViewById(R.id.btnReset).setOnClickListener(v -> {
            adv = null;
            filter(edtSearch.getText().toString());
            dialog.dismiss();
        });
        sheet.findViewById(R.id.btnApply).setOnClickListener(v -> {
            HikeFilter f = new HikeFilter();
            f.name = str(name);
            f.location = str(location);
            f.minKm = dbl(minKm);
            f.maxKm = dbl(maxKm);
            f.from = str(from);
            f.to = str(to);
            adv = f;
            filter(edtSearch.getText().toString());
            dialog.dismiss();
        });
        dialog.show();
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) ->
                target.setText(String.format(Locale.UK, "%04d-%02d-%02d", y, m + 1, d)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static String str(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Double dbl(EditText e) {
        String s = str(e);
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return null; }
    }
}
