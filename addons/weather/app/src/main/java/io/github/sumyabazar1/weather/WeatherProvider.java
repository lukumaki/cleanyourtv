package io.github.sumyabazar1.weather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.time.format.TextStyle;
import java.util.Locale;

/**
 * The weather now at the chosen place, for a launcher that shows it on Home (Clean Your TV: docs/modules.md,
 * "The weather on Home"). One read-only row at content://io.github.sumyabazar1.weather.feed/now. It holds only
 * what the page already shows: the place's name and the weather there, no account and no location.
 *
 * Nothing runs in the background: a query is the only thing that goes online, and the last reading is reused for
 * {@link #FEED_REFRESH_MS}. After a failed read the next try waits {@link Forecast#RETRY_MS}, and a reading
 * older than {@link #FEED_MAX_AGE_MS} is dropped rather than shown as current.
 */
public final class WeatherProvider extends ContentProvider {
    /**
     * The first eight are the weather now. The last four are tomorrow's: the weekday's short name in the TV's
     * language, the sky in words, and the high and low in degrees Celsius. They are empty/null when the forecast
     * has no tomorrow. New columns go on the end, so a launcher that reads the first eight by position still works.
     */
    static final String[] COLUMNS = {"place", "temp_c", "feels_c", "condition", "humidity", "wind_kmh", "aqi", "updated_ms",
            "tomorrow_day", "tomorrow_condition", "tomorrow_max_c", "tomorrow_min_c"};
    /**
     * /days: one row per day after today (tomorrow first, three of them): the weekday's short name and the sky in
     * the TV's language, the high and low in degrees Celsius, and the day's highest chance of precipitation in %
     * (null when the model gives none).
     */
    static final String[] DAY_COLUMNS = {"day", "condition", "max_c", "min_c", "rain_pct"};
    /** The feed reads the weather every 3 hours, not as often as the page does: a launcher asking again costs nothing. */
    static final long FEED_REFRESH_MS = 3 * 60 * 60_000L;
    /** A reading is dropped after twice that, so one failed refresh does not blank the line. */
    static final long FEED_MAX_AGE_MS = 2 * FEED_REFRESH_MS;

    private static Forecast.Report last;
    private static long lastAt;
    private static double lastLat, lastLon;
    private static long failedAt;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder) {
        boolean days = "/days".equals(uri.getPath());
        MatrixCursor out = new MatrixCursor(days ? DAY_COLUMNS : COLUMNS);
        if (getContext() == null || !days && !"/now".equals(uri.getPath())) return out;
        SharedPreferences place = getContext().getSharedPreferences("place", android.content.Context.MODE_PRIVATE);
        if (!place.contains("name")) return out;
        double lat = Double.longBitsToDouble(place.getLong("lat", 0));
        double lon = Double.longBitsToDouble(place.getLong("lon", 0));
        Forecast.Report r = report(lat, lon);
        if (r == null) return out;
        if (days) {
            for (int i = 0; i < r.days.size() && i < Forecast.MIN_DAYS; i++) {
                Forecast.Day d = r.days.get(i);
                out.addRow(new Object[] {d.date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        getContext().getString(Forecast.sky(d.code, true)), d.maxC, d.minC,
                        d.rainPct < 0 ? null : d.rainPct});
            }
            return out;
        }
        Forecast.Day tomorrow = r.days.isEmpty() ? null : r.days.get(0);
        out.addRow(new Object[] {place.getString("name", ""), r.tempC, r.feelsC,
                getContext().getString(Forecast.sky(r.code, r.day)), r.humidity, r.windKmh, r.aqi, lastAt,
                tomorrow == null ? "" : tomorrow.date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                tomorrow == null ? "" : getContext().getString(Forecast.sky(tomorrow.code, true)),
                tomorrow == null ? null : tomorrow.maxC, tomorrow == null ? null : tomorrow.minC});
        return out;
    }

    /** The cached reading while it is fresh, else a new one; null when there is none worth showing. */
    private static synchronized Forecast.Report report(double lat, double lon) {
        long now = System.currentTimeMillis();
        boolean same = last != null && lastLat == lat && lastLon == lon;
        if (same && now - lastAt < FEED_REFRESH_MS) return last;
        if (now - failedAt >= Forecast.RETRY_MS) {
            try {
                last = Forecast.fetch(lat, lon, Forecast.MIN_DAYS);
                lastAt = now;
                lastLat = lat;
                lastLon = lon;
                return last;
            } catch (Exception e) {
                failedAt = now;
            }
        }
        return same && now - lastAt < FEED_MAX_AGE_MS ? last : null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }
}
