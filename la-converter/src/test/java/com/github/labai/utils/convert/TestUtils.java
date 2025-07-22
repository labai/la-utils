package com.github.labai.utils.convert;

import java.util.TimeZone;

/*
 * @author Augustus
 * created on 2025-07-06
 */
public class TestUtils {

    // use local zone always +02:00
    public static void withTimeZone0200(Runnable runnable) {
        var currZone = TimeZone.getDefault();
        try {
            // timezone with no daylight saving
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"));
            runnable.run();
        } finally {
            TimeZone.setDefault(currZone);
        }
    }
}
