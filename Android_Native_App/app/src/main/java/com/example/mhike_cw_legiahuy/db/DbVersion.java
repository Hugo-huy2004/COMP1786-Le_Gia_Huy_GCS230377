package com.example.mhike_cw_legiahuy.db;

public final class DbVersion {
    private static volatile int version = 0;
    private DbVersion() {}

    public static int get() { return version; }

    public static void bump() { version++; }
}
