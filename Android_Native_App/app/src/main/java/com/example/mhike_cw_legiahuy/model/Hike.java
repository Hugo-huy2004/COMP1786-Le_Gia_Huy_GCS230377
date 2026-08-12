package com.example.mhike_cw_legiahuy.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.util.List;

/**
 * Field names here follow the snake_case layout the web app already writes to
 * /users/&lt;googleId&gt;/hikes. Changing a @PropertyName breaks compatibility with it.
 */
@IgnoreExtraProperties
public class Hike implements Serializable {
    /** Push key, filled in from the snapshot. The stored "id" is a number, so keep it out of mapping. */
    @Exclude public String id;

    public String name;
    public String location;
    public String difficulty;
    public String description;
    public String weather;
    public double latitude;
    public double longitude;

    @PropertyName("hike_date") public String hikeDate;
    @PropertyName("length_km") public double lengthKm;
    @PropertyName("start_time") public String startTime;
    @PropertyName("trail_type") public String trailType;
    public String status;

    /** Web app stores these as 0/1 rather than booleans, so read them leniently. */
    @Exclude public boolean parking;
    @Exclude public boolean favourite;
    /** Web app stores hours as a number; the UI wants a string. */
    @Exclude public String duration;

    public List<String> kitList;
    public List<String> photos;
    public String priority;
    public String visibility;
    @PropertyName("emergency_contact") public String emergencyContact;

    public Hike() {}

    @PropertyName("parking")
    public long getParkingRaw() {
        return parking ? 1 : 0;
    }

    @PropertyName("parking")
    public void setParkingRaw(Object v) {
        parking = asBool(v);
    }

    @PropertyName("favourite")
    public long getFavouriteRaw() {
        return favourite ? 1 : 0;
    }

    @PropertyName("favourite")
    public void setFavouriteRaw(Object v) {
        favourite = asBool(v);
    }

    @PropertyName("duration_hours")
    public String getDurationRaw() {
        return duration;
    }

    @PropertyName("duration_hours")
    public void setDurationRaw(Object v) {
        duration = v == null ? null : String.valueOf(v);
    }

    @Exclude
    private static boolean asBool(Object v) {
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "1".equals(v) || "true".equalsIgnoreCase((String) v);
        return false;
    }
}
