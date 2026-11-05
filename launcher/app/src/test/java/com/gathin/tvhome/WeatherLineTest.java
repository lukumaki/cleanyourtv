package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class WeatherLineTest {
    private static WeatherLine.Reading reading(String place, double tempC, double feelsC, int humidity) {
        return new WeatherLine.Reading(place, tempC, feelsC, humidity);
    }

    private static WeatherLine.Day day(String name, double max, double min, int rain) {
        return new WeatherLine.Day(name, max, min, rain);
    }

    @Test public void todayShowsPlaceTemperatureHumidityAndFeelsLike() {
        assertArrayEquals(new String[] {"Lisbon", "15°", "77%", "feels 14°"},
                WeatherLine.todayParts(reading("Lisbon", 15.2, 13.6, 77)));
    }

    @Test public void todayRoundsDegreesToWholeNumbers() {
        assertArrayEquals(new String[] {"Athens", "16°", "50%", "feels 16°"},
                WeatherLine.todayParts(reading("Athens", 15.5, 15.6, 50)));
    }

    @Test public void todayRoundsBelowZeroTowardTheNearestDegree() {
        assertArrayEquals(new String[] {"Oslo", "-2°", "90%", "feels -5°"},
                WeatherLine.todayParts(reading("Oslo", -2.4, -4.6, 90)));
    }

    @Test public void daysShowWeekdayHighAndLow() {
        assertArrayEquals(new String[] {"Thu 18°/11°", "Fri 17°/10°", "Sat 16°/9°"},
                WeatherLine.daysParts(Arrays.asList(day("Thu", 17.5, 11.4, 0), day("Fri", 17.2, 10.1, 20), day("Sat", 15.9, 9.4, -1))));
    }

    @Test public void rainShowsFromFiftyPercent() {
        assertArrayEquals(new String[] {"Thu 18°/11°", "Fri 17°/10° rain 50%", "Sat 16°/9° rain 80%"},
                WeatherLine.daysParts(Arrays.asList(day("Thu", 17.5, 11.4, 49), day("Fri", 17.2, 10.1, 50), day("Sat", 15.9, 9.4, 80))));
    }

    @Test public void aDayWithoutValuesIsLeftOut() {
        assertArrayEquals(new String[] {"Fri 17°/10°"},
                WeatherLine.daysParts(Arrays.asList(day("", 17, 10, 0), day("Thu", Double.NaN, 9, 0), day("Fri", 17, 10, 0))));
    }

    @Test public void noDaysGivesNoParts() {
        assertArrayEquals(new String[0], WeatherLine.daysParts(Collections.<WeatherLine.Day>emptyList()));
    }
}
