# This fork: a weather pill with a three-day forecast

A fork of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) that adds **a glass weather pill below Home's clock and
date, showing the weather now and the next three days from the Weather add-on**. The launcher keeps no
`INTERNET` permission and goes online for nothing; the add-on does the fetching.

![Clock-width glass weather pill on a TV, with the city and Wi-Fi name blurred](docs/switches/weather_feed.jpg)

```
Lisbon  ·  13°  ·  86%  ·  feels 13°
Fri 16°/8°  ·  Sat 16°/7° rain 60%  ·  Sun 15°/7°
```

The first part is the place, the temperature, the humidity and the feels-like temperature. The second is the next three
days with their high and low; a day shows its chance of rain only from 50%, so a dry week stays short. The pill matches
the clock-and-date area's width, keeps the top buttons' glass fill and focus style, and centers its text as it wraps.
Press OK on it to open the Weather add-on's full page.

## See exactly what changed

This fork began at **v3.3.0** and has since merged upstream `main` through commit `b9538048`.
[Compare the weather-feed branch with upstream main](https://github.com/Gathin23/cleanyourtv/compare/main...lukumaki:weather-feed).
The README keeps the upstream text under a short fork note.

## What is different

| Part | What it does |
|---|---|
| `weather_feed` (launcher, `config.xml`) | The package of an add-on that publishes the weather. This fork names the Weather add-on by default; clear the value to hide the pill. If the add-on is absent, or another app holds its provider, Home shows no weather. |
| `wifi_status_color` (launcher, `config.xml`) | The Wi-Fi icon is green while the TV is online, making the connected state visible at a glance. |
| `addons/weather` (the Weather add-on) | Publishes a read-only content provider, `io.github.sumyabazar1.weather.feed`, with `/now` (place, temperature, feels-like, humidity, wind, air quality) and `/days` (weekday, sky, high, low, chance of rain). It holds only a place name and the weather there. |
| Greek launcher translation | Adds a complete Greek interface. For Greek, this fork uses a subset of [Arimo](https://fonts.google.com/specimen/Arimo), including its OFL license in the app assets; because Arimo starts at regular weight, the lighter clock styles also render at regular weight. |

The columns are in [docs/modules.md](docs/modules.md), "The weather on Home". The add-on asks for one more permission than
before, `ACCESS_NETWORK_STATE`, only to read whether Android has validated the network; the launcher's manifest is unchanged. They are not the `weather_now` columns the
other add-on-fed forks use; they carry the three days and the chance of rain too.

How often it reads, so the TV is not kept online for it:

- The launcher asks when Home comes to the front and every 3 hours while Home is on screen, on its own thread.
- The add-on reuses its last reading for 3 hours and goes online only when asked and the reading is older. It asks
  Android whether the network is validated first, so it does not try in the seconds after a boot when the Wi-Fi is up
  and the internet is not; waiting for that costs nothing, and on a network Android never validates it tries anyway
  after 2 minutes. After a fetch that really failed it waits 5 minutes before the next try. The launcher asks again
  after 1 minute when it got nothing.
- A reading older than 6 hours is dropped and the lines disappear, rather than show an old temperature as current.

## Tested on

A Tesla Smart TV Q55E655GUS (Expressluck, sepolia, MediaTek mt5896), Google TV 14, with this launcher as Home. The
glass, clock-width pill was checked on the TV in Greek with a 24-hour clock: the weather stayed centered while scrolling
the app grid, and OK opened the Weather add-on. The Greek Arimo build also installed and rendered the launcher UI and
weather pill on this TV. Other TVs have not been tried.

The weekday names follow the TV's language. The launcher translates the weather labels for its supported languages and
Greek. The add-on's full-page sky words fall back to English in Greek.

## Install

1. Build the launcher from this fork (`launcher/`, JDK 17 and the Android SDK) and install it as the Home app, as the main
   repo's README describes.
2. Build and install the add-on: `cd addons/weather && ./gradlew assembleRelease`, then `adb install -r` the APK. Open
   it once and pick your city.
3. The launcher's `weather_feed` key already names this add-on. To build without the weather pill, clear that value in
   `launcher/app/src/main/res/values/config.xml` before building.

Nothing else in the launcher changes, so an existing theme or list keeps working.

## Credits

The launcher, its tools and the Weather add-on are the work of [Gathin23](https://github.com/Gathin23) and
[Sumyabazar1](https://github.com/Sumyabazar1). Other forks that read the weather from an add-on were a model for
keeping the launcher offline: see [SHOWCASE.md](SHOWCASE.md) upstream. MIT, as the repo it forks.
