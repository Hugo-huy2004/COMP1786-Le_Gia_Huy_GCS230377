package com.example.mhike_cw_legiahuy.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.mhike_cw_legiahuy.model.Hike;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabase extends SQLiteOpenHelper {
    private static final String DBNAME = "mhike_local.db";
    private static final int VER = 1;

    public LocalDatabase(Context context) {
        super(context, DBNAME, null, VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE hikes (id TEXT PRIMARY KEY, name TEXT, location TEXT, hikeDate TEXT, " +
                "lengthKm REAL, difficulty TEXT, parking INTEGER, description TEXT, " +
                "trailType TEXT, weather TEXT, duration TEXT, latitude REAL, longitude REAL, priority TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS hikes");
        onCreate(db);
    }

    public void saveHikes(List<Hike> hikes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("hikes", null, null);
            for (Hike h : hikes) {
                ContentValues v = new ContentValues();
                v.put("id", h.id);
                v.put("name", h.name);
                v.put("location", h.location);
                v.put("hikeDate", h.hikeDate);
                v.put("lengthKm", h.lengthKm);
                v.put("difficulty", h.difficulty);
                v.put("parking", h.parking ? 1 : 0);
                v.put("description", h.description);
                v.put("trailType", h.trailType);
                v.put("weather", h.weather);
                v.put("duration", h.duration);
                v.put("latitude", h.latitude);
                v.put("longitude", h.longitude);
                v.put("priority", h.priority);
                db.insertWithOnConflict("hikes", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Hike> getAllHikes() {
        List<Hike> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM hikes ORDER BY hikeDate DESC", null);
        if (c.moveToFirst()) {
            do {
                Hike h = new Hike();
                h.id = c.getString(0);
                h.name = c.getString(1);
                h.location = c.getString(2);
                h.hikeDate = c.getString(3);
                h.lengthKm = c.getDouble(4);
                h.difficulty = c.getString(5);
                h.parking = c.getInt(6) == 1;
                h.description = c.getString(7);
                h.trailType = c.getString(8);
                h.weather = c.getString(9);
                h.duration = c.getString(10);
                h.latitude = c.getDouble(11);
                h.longitude = c.getDouble(12);
                h.priority = c.getString(13);
                list.add(h);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }
}
