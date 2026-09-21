package com.vitalis.ah.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Numbers, money, time formatting. */
public final class Fmt {
    private Fmt() {}

    public static String money(double v, String symbol) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(0);
        return nf.format(v) + symbol;
    }

    public static String timeLeft(long millis) {
        if (millis <= 0) return "expired";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }
}
