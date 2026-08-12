package com.example.mhike_cw_legiahuy.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.io.Serializable;

/**
 * Mirrors the web app's /users/&lt;googleId&gt;/observations layout.
 * What this app calls "comments" is stored there as "detail".
 */
@IgnoreExtraProperties
public class Observation implements Serializable {
    /** Push key, filled in from the snapshot. The stored "id" is a number, so keep it out of mapping. */
    @Exclude public String id;

    public String observation;
    public String wildlife;

    /** Web app writes hike_id as a number; this app matches it against push keys as text. */
    @Exclude public String hikeId;

    @PropertyName("obs_time") public String obsTime;
    @PropertyName("detail") public String comments;
    @PropertyName("trail_condition") public String trailCondition;
    public String vegetation;
    public String mood;
    public long rating;

    public Observation() {}

    @PropertyName("hike_id")
    public String getHikeIdRaw() {
        return hikeId;
    }

    @PropertyName("hike_id")
    public void setHikeIdRaw(Object v) {
        hikeId = v == null ? null : String.valueOf(v);
    }
}
