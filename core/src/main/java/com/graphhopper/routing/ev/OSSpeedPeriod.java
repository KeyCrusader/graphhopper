package com.graphhopper.routing.ev;

/*
 * Lists out all of the possible time periods.
 * The enum name is the name of the encoded_value to add to the config.yml
 */

public enum OSSpeedPeriod {
    mo_fr_04_07("Mo-Fr 04:00-07:00"),
    mo_fr_07_09("Mo-Fr 07:00-09:00"),
    mo_fr_09_12("Mo-Fr 09:00-12:00"),
    mo_fr_12_14("Mo-Fr 12:00-14:00"),
    mo_fr_14_16("Mo-Fr 14:00-16:00"),
    mo_fr_16_19("Mo-Fr 16:00-19:00"),
    mo_fr_19_22("Mo-Fr 19:00-22:00"),
    mo_fr_22_04("Mo-Fr 22:00-04:00"),
    sa_su_04_07("Sa-Su 04:00-07:00"),
    sa_su_07_09("Sa-Su 07:00-09:00"),
    sa_su_09_12("Sa-Su 09:00-12:00"),
    sa_su_12_14("Sa-Su 12:00-14:00"),
    sa_su_14_16("Sa-Su 14:00-16:00"),
    sa_su_16_19("Sa-Su 16:00-19:00"),
    sa_su_19_22("Sa-Su 19:00-22:00"),
    sa_su_22_04("Sa-Su 22:00-04:00");

    public final String tag;
    
    OSSpeedPeriod(String tag) {
        this.tag = tag;
    }

    public static boolean contains(String name) {
        for (OSSpeedPeriod period : OSSpeedPeriod.values()) {
            if (period.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
