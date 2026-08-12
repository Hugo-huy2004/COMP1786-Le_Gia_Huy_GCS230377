package com.example.mhike_cw_legiahuy.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * OpenStreetMap tiles through osmdroid: no API key and no billing account, unlike the
 * Google Maps SDK. Location still comes from Play Services, which needs neither.
 */
public class MapActivity extends BaseActivity {

    private static final int PERMISSION_CODE = 101;

    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView txtGps;
    private EditText edtSearch;
    private Marker searchMarker;

    /**
     * CARTO's free basemap: no API key and no billing, and unlike OSM's own volunteer servers
     * it does not 403 an app like this one. Attribution is shown on screen, as their terms ask.
     */
    private static final XYTileSource CARTO_LIGHT = new XYTileSource("CartoLight", 0, 20, 256, ".png",
            new String[]{
                    "https://a.basemaps.cartocdn.com/light_all/",
                    "https://b.basemaps.cartocdn.com/light_all/",
                    "https://c.basemaps.cartocdn.com/light_all/"},
            "© OpenStreetMap contributors © CARTO");

    private static final XYTileSource CARTO_DARK = new XYTileSource("CartoDark", 0, 20, 256, ".png",
            new String[]{
                    "https://a.basemaps.cartocdn.com/dark_all/",
                    "https://b.basemaps.cartocdn.com/dark_all/",
                    "https://c.basemaps.cartocdn.com/dark_all/"},
            "© OpenStreetMap contributors © CARTO");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        // Tile servers want an identifying user agent, not the library default.
        Configuration.getInstance().setUserAgentValue(getPackageName());
        // Internal storage: the tile cache is SQLite, and SQLite on emulated external storage
        // fails its ioctls (see the avc denials on /storage/emulated/0/.../tiles/cache.db).
        File base = new File(getFilesDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));
        setContentView(R.layout.activity_map);

        txtGps = findViewById(R.id.txtGpsInfo);
        edtSearch = findViewById(R.id.edtSearchPlace);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        map = findViewById(R.id.map);
        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        map.setTileSource(night ? CARTO_DARK : CARTO_LIGHT);
        map.setMultiTouchControls(true);
        map.getController().setZoom(5.0);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> getCurrentLocation());
        findViewById(R.id.btnSearchPlace).setOnClickListener(v -> searchPlace());

        getCurrentLocation();
        loadHikesOnMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDetach();
    }

    private void searchPlace() {
        String query = edtSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        // Geocoder is in the platform - no extra Places dependency for a single lookup.
        List<Address> results;
        try {
            results = new Geocoder(this, Locale.getDefault()).getFromLocationName(query, 1);
        } catch (IOException e) {
            Toast.makeText(this, R.string.geocoder_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        if (results == null || results.isEmpty()) {
            Toast.makeText(this, R.string.place_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        Address a = results.get(0);
        GeoPoint target = new GeoPoint(a.getLatitude(), a.getLongitude());

        // One reusable pin, so repeated searches do not litter the map.
        if (searchMarker == null) {
            searchMarker = new Marker(map);
            searchMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(searchMarker);
        }
        searchMarker.setPosition(target);
        searchMarker.setTitle(query);

        map.getController().animateTo(target);
        map.getController().setZoom(13.0);
        showGps(target);
    }

    private void loadHikesOnMap() {
        FirebaseHelper.hikes().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Drop only the hike pins, keeping the search marker and location overlay.
                map.getOverlays().removeAll(hikeMarkers());
                for (DataSnapshot s : snapshot.getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h == null || (h.latitude == 0 && h.longitude == 0)) continue;
                    Marker m = new Marker(map);
                    m.setPosition(new GeoPoint(h.latitude, h.longitude));
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    m.setTitle(h.name);
                    m.setSnippet(h.location);
                    m.setId(HIKE_PIN);
                    map.getOverlays().add(m);
                }
                map.invalidate();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("MHike", "map hikes cancelled: " + error.getMessage());
            }
        });
    }

    private static final String HIKE_PIN = "hike";

    private List<org.osmdroid.views.overlay.Overlay> hikeMarkers() {
        List<org.osmdroid.views.overlay.Overlay> out = new java.util.ArrayList<>();
        for (org.osmdroid.views.overlay.Overlay o : map.getOverlays()) {
            if (o instanceof Marker && HIKE_PIN.equals(((Marker) o).getId())) out.add(o);
        }
        return out;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location == null) return;
            GeoPoint current = new GeoPoint(location.getLatitude(), location.getLongitude());
            map.getController().animateTo(current);
            map.getController().setZoom(15.0);
            showGps(current);
        });
    }

    private void showGps(GeoPoint p) {
        txtGps.setText(String.format(Locale.UK, "GPS: %.4f, %.4f", p.getLatitude(), p.getLongitude()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
