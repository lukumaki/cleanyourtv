package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * weather_feed (config.xml): the weather now and the next three days, two lines under the clock, from an add-on such
 * as Weather. The launcher only reads what the add-on publishes and stays offline; the add-on does the fetching.
 * Contract: docs/modules.md.
 */
final class WeatherLine {
    static final String META = "com.gathin.tvhome.weather";
    /** Read again this often while Home is on screen: the add-on caches for as long, so most reads cost nothing. */
    private static final long EVERY_MS = 3 * 60 * 60_000L;
    /** Asked again this soon when the add-on had nothing (a failed fetch on its side, or no city chosen yet) or the query failed. */
    private static final long RETRY_MS = 5 * 60_000L;
    /** A day shows its chance of rain from this many % up. */
    static final int RAIN_FROM_PCT = 50;
    /** Between the parts of a line, set in bold. */
    private static final String SEPARATOR = "·";

    private final Context ctx;
    private final Uri uri;
    private final Uri daysUri;
    private final Handler main;
    private final Runnable onChange;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable again = this::read;
    private boolean running;
    private CharSequence text = "";

    private WeatherLine(Context ctx, Uri uri, Uri daysUri, Handler main, Runnable onChange) {
        this.ctx = ctx;
        this.uri = uri;
        this.daysUri = daysUri;
        this.main = main;
        this.onChange = onChange;
    }

    /** Null when the key is empty, the package is not installed, or it names no provider of its own in a {@value #META} meta-data. */
    static WeatherLine create(Context ctx, String pkg, Handler main, Runnable onChange) {
        if (pkg == null || pkg.trim().isEmpty()) return null;
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg.trim(), PackageManager.GET_META_DATA);
            String authority = info.metaData == null ? null : info.metaData.getString(META);
            if (authority == null || authority.trim().isEmpty()) return null;
            // The provider must be the named package's own, not another app's that claims the authority.
            ProviderInfo provider = pm.resolveContentProvider(authority.trim(), 0);
            if (provider == null || !pkg.trim().equals(provider.packageName)) return null;
            Uri.Builder base = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority.trim());
            return new WeatherLine(ctx, base.build().buildUpon().appendPath("now").build(),
                    base.build().buildUpon().appendPath("days").build(), main, onChange);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Home is on screen: read now, then every {@link #EVERY_MS}. */
    void start() {
        running = true;
        read();
    }

    /** Home is off screen: nothing runs until it is back. */
    void stop() {
        running = false;
        main.removeCallbacks(again);
    }

    void shutdown() {
        stop();
        io.shutdownNow();
    }

    /** The weather now, then the next three days on a second line; empty while there is nothing to show. */
    CharSequence text() {
        return text;
    }

    /** One query off the main thread (the provider lives in the add-on's process), then onChange on the main thread. */
    private void read() {
        main.removeCallbacks(again);
        try {
            io.execute(() -> {
                Reading reading = null;
                List<Day> days = new ArrayList<>();
                try (Cursor c = ctx.getContentResolver().query(uri, null, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        reading = new Reading(c.getString(0), c.getDouble(1), c.getDouble(2), c.getInt(4));
                    }
                } catch (RuntimeException gone) {
                    // Uninstalled or crashed mid-query: keep what is on screen, and ask again soon.
                    retrySoon();
                    return;
                }
                try (Cursor c = ctx.getContentResolver().query(daysUri, null, null, null, null)) {
                    while (c != null && c.moveToNext()) {
                        days.add(new Day(c.getString(0), c.isNull(2) ? Double.NaN : c.getDouble(2),
                                c.isNull(3) ? Double.NaN : c.getDouble(3), c.isNull(4) ? -1 : c.getInt(4)));
                    }
                } catch (RuntimeException gone) {
                    retrySoon();
                    return;
                }
                CharSequence now = reading == null ? "" : join(todayParts(reading));
                CharSequence next = reading == null ? "" : join(daysParts(days));
                SpannableStringBuilder both = new SpannableStringBuilder(now);
                if (now.length() > 0 && next.length() > 0) both.append('\n');
                both.append(next);
                boolean empty = reading == null;
                main.post(() -> {
                    text = both;
                    onChange.run();
                    if (running) main.postDelayed(again, empty ? RETRY_MS : EVERY_MS);
                });
            });
        } catch (RejectedExecutionException shutDown) {
            // Home is being destroyed.
        }
    }

    /** A failed query: try again in {@link #RETRY_MS}, while Home is on screen. */
    private void retrySoon() {
        main.post(() -> {
            if (running) main.postDelayed(again, RETRY_MS);
        });
    }

    /** What the add-on publishes for now, as far as the line uses it: degrees Celsius as read, the humidity in %. */
    static final class Reading {
        final String place;
        final double tempC;
        final double feelsC;
        final int humidity;

        Reading(String place, double tempC, double feelsC, int humidity) {
            this.place = place;
            this.tempC = tempC;
            this.feelsC = feelsC;
            this.humidity = humidity;
        }
    }

    /** One forecast day: its short weekday name in the add-on's language, the high and low in degrees Celsius, the chance of rain in % (-1 unknown). */
    static final class Day {
        final String name;
        final double maxC;
        final double minC;
        final int rainPct;

        Day(String name, double maxC, double minC, int rainPct) {
            this.name = name == null ? "" : name;
            this.maxC = maxC;
            this.minC = minC;
            this.rainPct = rainPct;
        }
    }

    /** The first line: the place, the temperature, the relative humidity and the feels-like temperature. */
    static String[] todayParts(Reading r) {
        return new String[] {r.place, Math.round(r.tempC) + "°", r.humidity + "%", "feels " + Math.round(r.feelsC) + "°"};
    }

    /**
     * The second line, one part per day, e.g. "Thu 18°/11°" and "Fri 17°/10° rain 60%". The chance of rain is added
     * only from {@link #RAIN_FROM_PCT} %, so a dry week stays short. A day without a name or a high and low is left out.
     */
    static String[] daysParts(List<Day> days) {
        List<String> out = new ArrayList<>();
        for (Day d : days) {
            if (d.name.isEmpty() || Double.isNaN(d.maxC) || Double.isNaN(d.minC)) continue;
            String part = d.name + " " + Math.round(d.maxC) + "°/" + Math.round(d.minC) + "°";
            if (d.rainPct >= RAIN_FROM_PCT) part += " rain " + d.rainPct + "%";
            out.add(part);
        }
        return out.toArray(new String[0]);
    }

    /** The parts as one line with a bold dot between them; empty when there are none. */
    static CharSequence join(String[] parts) {
        SpannableStringBuilder line = new SpannableStringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (line.length() > 0) {
                line.append("  ");
                line.append(SEPARATOR);
                line.setSpan(new StyleSpan(Typeface.BOLD), line.length() - 1, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                line.append("  ");
            }
            line.append(parts[i]);
        }
        return line;
    }
}
