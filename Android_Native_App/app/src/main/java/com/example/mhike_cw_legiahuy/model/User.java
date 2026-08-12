package com.example.mhike_cw_legiahuy.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

/**
 * Mirrors /users/&lt;googleId&gt;/profile as written by the web app.
 * BMI is derived rather than stored - the web app does not keep a bmi field.
 */
@IgnoreExtraProperties
public class User {
    public String name;
    public String email;
    public String gender;
    public int age;

    @PropertyName("height_cm") public double heightCm;
    @PropertyName("weight_kg") public double weightKg;

    public User() {}

    @Exclude
    public double bmi() {
        if (heightCm <= 0) return 0;
        double m = heightCm / 100.0;
        return weightKg / (m * m);
    }
}
